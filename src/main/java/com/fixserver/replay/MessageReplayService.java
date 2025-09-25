package com.fixserver.replay;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.session.FIXSession;
import com.fixserver.store.MessageStore;
import com.fixserver.store.entity.AuditRecordEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling FIX message replay functionality
 */
@Slf4j
@Service
public class MessageReplayService {
    
    private final MessageStore messageStore;
    
    @Autowired
    public MessageReplayService(MessageStore messageStore) {
        this.messageStore = messageStore;
    }
    
    /**
     * Handle a resend request from a counterparty
     */
    public CompletableFuture<ReplayResult> handleResendRequest(FIXSession session, 
                                                              int beginSeqNo, 
                                                              int endSeqNo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sessionId = session.getSessionId();
                log.info("Processing resend request for session {}: {} to {}", 
                        sessionId, beginSeqNo, endSeqNo);
                
                // Validate sequence number range
                if (beginSeqNo <= 0 || (endSeqNo != 0 && endSeqNo < beginSeqNo)) {
                    log.warn("Invalid sequence number range for session {}: {} to {}", 
                            sessionId, beginSeqNo, endSeqNo);
                    return ReplayResult.error("Invalid sequence number range");
                }
                
                // Handle infinite end sequence (0 means infinity in FIX)
                int actualEndSeqNo = endSeqNo == 0 ? Integer.MAX_VALUE : endSeqNo;
                
                // Get the last sent sequence number to limit replay
                int lastSentSeqNo = messageStore != null ? 
                    messageStore.getLastSequenceNumber(sessionId, MessageStore.MessageDirection.OUTGOING) : 0;
                
                if (beginSeqNo > lastSentSeqNo) {
                    log.warn("Begin sequence number {} is beyond last sent {} for session {}", 
                            beginSeqNo, lastSentSeqNo, sessionId);
                    return ReplayResult.error("Begin sequence number beyond last sent");
                }
                
                // Limit end sequence to last sent
                actualEndSeqNo = Math.min(actualEndSeqNo, lastSentSeqNo);
                
                // Retrieve messages for replay
                List<FIXMessage> messagesToReplay = messageStore != null ? 
                    messageStore.getMessages(sessionId, beginSeqNo, actualEndSeqNo) : 
                    new java.util.ArrayList<>();
                
                log.info("Found {} messages to replay for session {} (seq {} to {})", 
                        messagesToReplay.size(), sessionId, beginSeqNo, actualEndSeqNo);
                
                // Process each message for replay
                int replayedCount = 0;
                int gapFillCount = 0;
                
                for (int seqNo = beginSeqNo; seqNo <= actualEndSeqNo; seqNo++) {
                    FIXMessage messageToReplay = findMessageBySequence(messagesToReplay, seqNo);
                    
                    if (messageToReplay != null) {
                        // Replay the actual message
                        replayMessage(session, messageToReplay);
                        replayedCount++;
                    } else {
                        // Send gap fill for missing message
                        sendGapFill(session, seqNo);
                        gapFillCount++;
                    }
                }
                
                // Create audit record
                createReplayAuditRecord(sessionId, beginSeqNo, actualEndSeqNo, 
                                      replayedCount, gapFillCount);
                
                return ReplayResult.success(replayedCount, gapFillCount, beginSeqNo, actualEndSeqNo);
                
            } catch (Exception e) {
                log.error("Error handling resend request for session {}: {}", 
                         session.getSessionId(), e.getMessage(), e);
                return ReplayResult.error("Internal error during replay: " + e.getMessage());
            }
        });
    }
    
    /**
     * Replay messages for session recovery after reconnection
     */
    public CompletableFuture<ReplayResult> replayMessagesForRecovery(FIXSession session, 
                                                                   int fromSeqNo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sessionId = session.getSessionId();
                log.info("Replaying messages for session recovery {}: from seq {}", 
                        sessionId, fromSeqNo);
                
                int lastSentSeqNo = messageStore != null ? 
                    messageStore.getLastSequenceNumber(sessionId, MessageStore.MessageDirection.OUTGOING) : 0;
                
                if (fromSeqNo > lastSentSeqNo) {
                    log.info("No messages to replay for session {} (from {} > last {})", 
                            sessionId, fromSeqNo, lastSentSeqNo);
                    return ReplayResult.success(0, 0, fromSeqNo, fromSeqNo);
                }
                
                List<FIXMessage> messagesToReplay = messageStore != null ? 
                    messageStore.getMessages(sessionId, fromSeqNo, lastSentSeqNo) : 
                    new java.util.ArrayList<>();
                
                int replayedCount = 0;
                for (FIXMessage message : messagesToReplay) {
                    replayMessage(session, message);
                    replayedCount++;
                }
                
                createRecoveryAuditRecord(sessionId, fromSeqNo, lastSentSeqNo, replayedCount);
                
                return ReplayResult.success(replayedCount, 0, fromSeqNo, lastSentSeqNo);
                
            } catch (Exception e) {
                log.error("Error replaying messages for recovery for session {}: {}", 
                         session.getSessionId(), e.getMessage(), e);
                return ReplayResult.error("Internal error during recovery replay: " + e.getMessage());
            }
        });
    }
    
    /**
     * Handle sequence reset request
     */
    public CompletableFuture<Void> handleSequenceReset(FIXSession session, 
                                                       int newSeqNo, 
                                                       boolean gapFillFlag) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sessionId = session.getSessionId();
                log.info("Processing sequence reset for session {}: new seq {}, gap fill: {}", 
                        sessionId, newSeqNo, gapFillFlag);
                
                if (gapFillFlag) {
                    // Gap fill sequence reset - fill the gap with sequence reset message
                    sendSequenceReset(session, newSeqNo, true);
                } else {
                    // Hard sequence reset - reset sequence numbers
                    sendSequenceReset(session, newSeqNo, false);
                }
                
                // Create audit record
                createSequenceResetAuditRecord(sessionId, newSeqNo, gapFillFlag);
                
            } catch (Exception e) {
                log.error("Error handling sequence reset for session {}: {}", 
                         session.getSessionId(), e.getMessage(), e);
                throw new RuntimeException("Failed to handle sequence reset", e);
            }
        });
    }
    
    /**
     * Get replay statistics for a session
     */
    public ReplayStatistics getReplayStatistics(String sessionId) {
        try {
            // This would typically query audit records for replay events
            // For now, return basic statistics
            return new ReplayStatistics(sessionId, 0, 0, 0, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error getting replay statistics for session {}: {}", sessionId, e.getMessage(), e);
            return new ReplayStatistics(sessionId, 0, 0, 0, null);
        }
    }
    
    private FIXMessage findMessageBySequence(List<FIXMessage> messages, int sequenceNumber) {
        return messages.stream()
            .filter(msg -> msg.getMessageSequenceNumber() == sequenceNumber)
            .findFirst()
            .orElse(null);
    }
    
    private void replayMessage(FIXSession session, FIXMessage message) {
        try {
            // Mark message as possible duplicate
            message.setField(43, "Y"); // PossDupFlag
            
            // Update sending time to current time
            message.setField(FIXMessage.SENDING_TIME, 
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")));
            
            // Send the message
            session.sendMessage(message);
            
            log.debug("Replayed message for session {}: seq {}, type {}", 
                     session.getSessionId(), message.getMessageSequenceNumber(), message.getMessageType());
            
        } catch (Exception e) {
            log.error("Error replaying message for session {}: {}", 
                     session.getSessionId(), e.getMessage(), e);
        }
    }
    
    private void sendGapFill(FIXSession session, int sequenceNumber) {
        try {
            FIXMessage gapFill = new FIXMessageImpl("FIX.4.4", "4"); // Sequence Reset
            gapFill.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(sequenceNumber));
            gapFill.setField(36, String.valueOf(sequenceNumber + 1)); // NewSeqNo
            gapFill.setField(123, "Y"); // GapFillFlag
            gapFill.setField(43, "Y"); // PossDupFlag
            
            session.sendMessage(gapFill);
            
            log.debug("Sent gap fill for session {}: seq {}", session.getSessionId(), sequenceNumber);
            
        } catch (Exception e) {
            log.error("Error sending gap fill for session {}: {}", 
                     session.getSessionId(), e.getMessage(), e);
        }
    }
    
    private void sendSequenceReset(FIXSession session, int newSeqNo, boolean gapFillFlag) {
        try {
            FIXMessage sequenceReset = new FIXMessageImpl("FIX.4.4", "4"); // Sequence Reset
            sequenceReset.setField(36, String.valueOf(newSeqNo)); // NewSeqNo
            sequenceReset.setField(123, gapFillFlag ? "Y" : "N"); // GapFillFlag
            
            session.sendMessage(sequenceReset);
            
            log.debug("Sent sequence reset for session {}: new seq {}, gap fill: {}", 
                     session.getSessionId(), newSeqNo, gapFillFlag);
            
        } catch (Exception e) {
            log.error("Error sending sequence reset for session {}: {}", 
                     session.getSessionId(), e.getMessage(), e);
        }
    }
    
    private void createReplayAuditRecord(String sessionId, int beginSeqNo, int endSeqNo, 
                                       int replayedCount, int gapFillCount) {
        try {
            if (messageStore instanceof com.fixserver.store.MessageStoreImpl) {
                com.fixserver.store.MessageStoreImpl storeImpl = 
                    (com.fixserver.store.MessageStoreImpl) messageStore;
                
                String description = String.format(
                    "Message replay: seq %d-%d, replayed %d messages, %d gap fills", 
                    beginSeqNo, endSeqNo, replayedCount, gapFillCount);
                
                storeImpl.createAuditRecord(sessionId, 
                    AuditRecordEntity.AuditEventType.RESEND_REQUEST, description);
            }
        } catch (Exception e) {
            log.warn("Failed to create replay audit record: {}", e.getMessage());
        }
    }
    
    private void createRecoveryAuditRecord(String sessionId, int fromSeqNo, int toSeqNo, 
                                         int replayedCount) {
        try {
            if (messageStore instanceof com.fixserver.store.MessageStoreImpl) {
                com.fixserver.store.MessageStoreImpl storeImpl = 
                    (com.fixserver.store.MessageStoreImpl) messageStore;
                
                String description = String.format(
                    "Session recovery replay: seq %d-%d, replayed %d messages", 
                    fromSeqNo, toSeqNo, replayedCount);
                
                storeImpl.createAuditRecord(sessionId, 
                    AuditRecordEntity.AuditEventType.SESSION_CREATED, description);
            }
        } catch (Exception e) {
            log.warn("Failed to create recovery audit record: {}", e.getMessage());
        }
    }
    
    private void createSequenceResetAuditRecord(String sessionId, int newSeqNo, boolean gapFillFlag) {
        try {
            if (messageStore instanceof com.fixserver.store.MessageStoreImpl) {
                com.fixserver.store.MessageStoreImpl storeImpl = 
                    (com.fixserver.store.MessageStoreImpl) messageStore;
                
                String description = String.format(
                    "Sequence reset: new seq %d, gap fill: %s", newSeqNo, gapFillFlag);
                
                storeImpl.createAuditRecord(sessionId, 
                    AuditRecordEntity.AuditEventType.SEQUENCE_RESET, description);
            }
        } catch (Exception e) {
            log.warn("Failed to create sequence reset audit record: {}", e.getMessage());
        }
    }
    
    /**
     * Result of a replay operation
     */
    public static class ReplayResult {
        private final boolean success;
        private final String errorMessage;
        private final int messagesReplayed;
        private final int gapFillsSent;
        private final int beginSeqNo;
        private final int endSeqNo;
        
        private ReplayResult(boolean success, String errorMessage, int messagesReplayed, 
                           int gapFillsSent, int beginSeqNo, int endSeqNo) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.messagesReplayed = messagesReplayed;
            this.gapFillsSent = gapFillsSent;
            this.beginSeqNo = beginSeqNo;
            this.endSeqNo = endSeqNo;
        }
        
        public static ReplayResult success(int messagesReplayed, int gapFillsSent, 
                                         int beginSeqNo, int endSeqNo) {
            return new ReplayResult(true, null, messagesReplayed, gapFillsSent, beginSeqNo, endSeqNo);
        }
        
        public static ReplayResult error(String errorMessage) {
            return new ReplayResult(false, errorMessage, 0, 0, 0, 0);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public int getMessagesReplayed() { return messagesReplayed; }
        public int getGapFillsSent() { return gapFillsSent; }
        public int getBeginSeqNo() { return beginSeqNo; }
        public int getEndSeqNo() { return endSeqNo; }
        
        @Override
        public String toString() {
            if (success) {
                return String.format("ReplayResult{success=true, replayed=%d, gapFills=%d, range=%d-%d}", 
                                   messagesReplayed, gapFillsSent, beginSeqNo, endSeqNo);
            } else {
                return String.format("ReplayResult{success=false, error='%s'}", errorMessage);
            }
        }
    }
    
    /**
     * Replay statistics for monitoring
     */
    public static class ReplayStatistics {
        private final String sessionId;
        private final int totalReplays;
        private final int totalMessagesReplayed;
        private final int totalGapFills;
        private final LocalDateTime lastReplayTime;
        
        public ReplayStatistics(String sessionId, int totalReplays, int totalMessagesReplayed, 
                              int totalGapFills, LocalDateTime lastReplayTime) {
            this.sessionId = sessionId;
            this.totalReplays = totalReplays;
            this.totalMessagesReplayed = totalMessagesReplayed;
            this.totalGapFills = totalGapFills;
            this.lastReplayTime = lastReplayTime;
        }
        
        public String getSessionId() { return sessionId; }
        public int getTotalReplays() { return totalReplays; }
        public int getTotalMessagesReplayed() { return totalMessagesReplayed; }
        public int getTotalGapFills() { return totalGapFills; }
        public LocalDateTime getLastReplayTime() { return lastReplayTime; }
    }
}