package com.fixserver.replay;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.session.FIXSession;
import com.fixserver.store.MessageStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages gap detection and filling for FIX message sequences
 */
@Slf4j
@Component
public class GapFillManager {
    
    private final MessageStore messageStore;
    
    @Autowired
    public GapFillManager(MessageStore messageStore) {
        this.messageStore = messageStore;
    }
    
    /**
     * Detect sequence gaps in incoming messages
     */
    public List<SequenceGap> detectGaps(String sessionId, int expectedSeqNo, int receivedSeqNo) {
        List<SequenceGap> gaps = new ArrayList<>();
        
        if (receivedSeqNo > expectedSeqNo) {
            // Gap detected
            SequenceGap gap = new SequenceGap(sessionId, expectedSeqNo, receivedSeqNo - 1);
            gaps.add(gap);
            
            log.info("Detected sequence gap for session {}: expected {}, received {}", 
                    sessionId, expectedSeqNo, receivedSeqNo);
        }
        
        return gaps;
    }
    
    /**
     * Request resend for detected gaps
     */
    public CompletableFuture<Void> requestResend(FIXSession session, SequenceGap gap) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Requesting resend for session {}: seq {} to {}", 
                        session.getSessionId(), gap.getBeginSeqNo(), gap.getEndSeqNo());
                
                FIXMessage resendRequest = createResendRequest(gap.getBeginSeqNo(), gap.getEndSeqNo());
                session.sendMessage(resendRequest);
                
                gap.setResendRequested(true);
                gap.setResendRequestTime(LocalDateTime.now());
                
            } catch (Exception e) {
                log.error("Error requesting resend for session {}: {}", 
                         session.getSessionId(), e.getMessage(), e);
                throw new RuntimeException("Failed to request resend", e);
            }
        });
    }
    
    /**
     * Process incoming sequence reset message
     */
    public CompletableFuture<SequenceResetResult> processSequenceReset(FIXSession session, 
                                                                       FIXMessage sequenceReset) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sessionId = session.getSessionId();
                String newSeqNoStr = sequenceReset.getField(36); // NewSeqNo
                String gapFillFlag = sequenceReset.getField(123); // GapFillFlag
                
                if (newSeqNoStr == null) {
                    log.warn("Sequence reset missing NewSeqNo field for session {}", sessionId);
                    return SequenceResetResult.error("Missing NewSeqNo field");
                }
                
                int newSeqNo;
                try {
                    newSeqNo = Integer.parseInt(newSeqNoStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid NewSeqNo in sequence reset for session {}: {}", sessionId, newSeqNoStr);
                    return SequenceResetResult.error("Invalid NewSeqNo: " + newSeqNoStr);
                }
                
                boolean isGapFill = "Y".equals(gapFillFlag);
                
                log.info("Processing sequence reset for session {}: new seq {}, gap fill: {}", 
                        sessionId, newSeqNo, isGapFill);
                
                if (isGapFill) {
                    // Gap fill sequence reset - update expected sequence number
                    return SequenceResetResult.gapFill(newSeqNo);
                } else {
                    // Hard sequence reset - reset sequence numbers
                    return SequenceResetResult.hardReset(newSeqNo);
                }
                
            } catch (Exception e) {
                log.error("Error processing sequence reset for session {}: {}", 
                         session.getSessionId(), e.getMessage(), e);
                return SequenceResetResult.error("Internal error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Check if a message fills a known gap
     */
    public boolean fillsGap(List<SequenceGap> gaps, int sequenceNumber) {
        return gaps.stream().anyMatch(gap -> 
            sequenceNumber >= gap.getBeginSeqNo() && sequenceNumber <= gap.getEndSeqNo());
    }
    
    /**
     * Update gaps after receiving a message
     */
    public List<SequenceGap> updateGaps(List<SequenceGap> gaps, int receivedSeqNo) {
        List<SequenceGap> updatedGaps = new ArrayList<>();
        
        for (SequenceGap gap : gaps) {
            if (receivedSeqNo >= gap.getBeginSeqNo() && receivedSeqNo <= gap.getEndSeqNo()) {
                // Message fills part of this gap
                if (receivedSeqNo == gap.getBeginSeqNo() && receivedSeqNo == gap.getEndSeqNo()) {
                    // Gap completely filled - remove it
                    log.debug("Gap completely filled: {}", gap);
                } else if (receivedSeqNo == gap.getBeginSeqNo()) {
                    // Fills beginning of gap
                    SequenceGap remainingGap = new SequenceGap(
                        gap.getSessionId(), receivedSeqNo + 1, gap.getEndSeqNo());
                    updatedGaps.add(remainingGap);
                    log.debug("Gap partially filled at beginning: {} -> {}", gap, remainingGap);
                } else if (receivedSeqNo == gap.getEndSeqNo()) {
                    // Fills end of gap
                    SequenceGap remainingGap = new SequenceGap(
                        gap.getSessionId(), gap.getBeginSeqNo(), receivedSeqNo - 1);
                    updatedGaps.add(remainingGap);
                    log.debug("Gap partially filled at end: {} -> {}", gap, remainingGap);
                } else {
                    // Fills middle of gap - split into two gaps
                    SequenceGap beforeGap = new SequenceGap(
                        gap.getSessionId(), gap.getBeginSeqNo(), receivedSeqNo - 1);
                    SequenceGap afterGap = new SequenceGap(
                        gap.getSessionId(), receivedSeqNo + 1, gap.getEndSeqNo());
                    updatedGaps.add(beforeGap);
                    updatedGaps.add(afterGap);
                    log.debug("Gap split: {} -> {} and {}", gap, beforeGap, afterGap);
                }
            } else {
                // Gap not affected by this message
                updatedGaps.add(gap);
            }
        }
        
        return updatedGaps;
    }
    
    /**
     * Get gap statistics for monitoring
     */
    public GapStatistics getGapStatistics(String sessionId, List<SequenceGap> gaps) {
        int totalGaps = gaps.size();
        int totalMissingMessages = gaps.stream()
            .mapToInt(gap -> gap.getEndSeqNo() - gap.getBeginSeqNo() + 1)
            .sum();
        
        long pendingResends = gaps.stream()
            .filter(SequenceGap::isResendRequested)
            .count();
        
        return new GapStatistics(sessionId, totalGaps, totalMissingMessages, (int) pendingResends);
    }
    
    private FIXMessage createResendRequest(int beginSeqNo, int endSeqNo) {
        FIXMessage resendRequest = new FIXMessageImpl("FIX.4.4", "2"); // Resend Request
        resendRequest.setField(7, String.valueOf(beginSeqNo)); // BeginSeqNo
        resendRequest.setField(16, String.valueOf(endSeqNo)); // EndSeqNo
        return resendRequest;
    }
    
    /**
     * Represents a sequence gap that needs to be filled
     */
    public static class SequenceGap {
        private final String sessionId;
        private final int beginSeqNo;
        private final int endSeqNo;
        private boolean resendRequested = false;
        private LocalDateTime detectedTime = LocalDateTime.now();
        private LocalDateTime resendRequestTime;
        
        public SequenceGap(String sessionId, int beginSeqNo, int endSeqNo) {
            this.sessionId = sessionId;
            this.beginSeqNo = beginSeqNo;
            this.endSeqNo = endSeqNo;
        }
        
        public String getSessionId() { return sessionId; }
        public int getBeginSeqNo() { return beginSeqNo; }
        public int getEndSeqNo() { return endSeqNo; }
        public boolean isResendRequested() { return resendRequested; }
        public LocalDateTime getDetectedTime() { return detectedTime; }
        public LocalDateTime getResendRequestTime() { return resendRequestTime; }
        
        public void setResendRequested(boolean resendRequested) { 
            this.resendRequested = resendRequested; 
        }
        
        public void setResendRequestTime(LocalDateTime resendRequestTime) { 
            this.resendRequestTime = resendRequestTime; 
        }
        
        public int getGapSize() {
            return endSeqNo - beginSeqNo + 1;
        }
        
        @Override
        public String toString() {
            return String.format("SequenceGap{session=%s, range=%d-%d, size=%d, requested=%s}", 
                               sessionId, beginSeqNo, endSeqNo, getGapSize(), resendRequested);
        }
    }
    
    /**
     * Result of processing a sequence reset message
     */
    public static class SequenceResetResult {
        private final boolean success;
        private final String errorMessage;
        private final int newSeqNo;
        private final boolean isGapFill;
        
        private SequenceResetResult(boolean success, String errorMessage, int newSeqNo, boolean isGapFill) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.newSeqNo = newSeqNo;
            this.isGapFill = isGapFill;
        }
        
        public static SequenceResetResult gapFill(int newSeqNo) {
            return new SequenceResetResult(true, null, newSeqNo, true);
        }
        
        public static SequenceResetResult hardReset(int newSeqNo) {
            return new SequenceResetResult(true, null, newSeqNo, false);
        }
        
        public static SequenceResetResult error(String errorMessage) {
            return new SequenceResetResult(false, errorMessage, 0, false);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public int getNewSeqNo() { return newSeqNo; }
        public boolean isGapFill() { return isGapFill; }
    }
    
    /**
     * Gap statistics for monitoring
     */
    public static class GapStatistics {
        private final String sessionId;
        private final int totalGaps;
        private final int totalMissingMessages;
        private final int pendingResends;
        
        public GapStatistics(String sessionId, int totalGaps, int totalMissingMessages, int pendingResends) {
            this.sessionId = sessionId;
            this.totalGaps = totalGaps;
            this.totalMissingMessages = totalMissingMessages;
            this.pendingResends = pendingResends;
        }
        
        public String getSessionId() { return sessionId; }
        public int getTotalGaps() { return totalGaps; }
        public int getTotalMissingMessages() { return totalMissingMessages; }
        public int getPendingResends() { return pendingResends; }
    }
}