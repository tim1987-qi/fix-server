package com.fixserver.session;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.protocol.MessageType;
import com.fixserver.store.MessageStore;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of FIX session with state management and message processing
 */
@Slf4j
public class FIXSessionImpl implements FIXSession {
    
    private final String sessionId;
    private final String senderCompId;
    private final String targetCompId;
    private final FIXProtocolHandler protocolHandler;
    private final MessageStore messageStore;
    private final int heartbeatInterval;
    
    private final AtomicReference<Status> status = new AtomicReference<>(Status.DISCONNECTED);
    private final AtomicInteger nextIncomingSeqNum = new AtomicInteger(1);
    private final AtomicInteger nextOutgoingSeqNum = new AtomicInteger(1);
    private final AtomicReference<LocalDateTime> lastHeartbeat = new AtomicReference<>(LocalDateTime.now());
    private final AtomicReference<LocalDateTime> sessionStartTime = new AtomicReference<>();
    
    // Session configuration
    private volatile boolean validateSequenceNumbers = true;
    private volatile boolean validateChecksum = true;
    
    public FIXSessionImpl(String sessionId, String senderCompId, String targetCompId,
                         FIXProtocolHandler protocolHandler, MessageStore messageStore,
                         int heartbeatInterval) {
        this.sessionId = sessionId;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.protocolHandler = protocolHandler;
        this.messageStore = messageStore;
        this.heartbeatInterval = heartbeatInterval;
        
        log.info("Created FIX session: {} ({}->{})", sessionId, senderCompId, targetCompId);
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public String getSenderCompId() {
        return senderCompId;
    }
    
    @Override
    public String getTargetCompId() {
        return targetCompId;
    }
    
    @Override
    public Status getStatus() {
        return status.get();
    }
    
    @Override
    public int getNextIncomingSequenceNumber() {
        return nextIncomingSeqNum.get();
    }
    
    @Override
    public int getNextOutgoingSequenceNumber() {
        return nextOutgoingSeqNum.get();
    }
    
    @Override
    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat.get();
    }
    
    @Override
    public LocalDateTime getSessionStartTime() {
        return sessionStartTime.get();
    }
    
    @Override
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    @Override
    public CompletableFuture<Void> processMessage(FIXMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing message in session {}: {}", sessionId, message);
                
                // Validate message
                if (!isValidMessage(message)) {
                    log.warn("Invalid message received in session {}: {}", sessionId, message);
                    sendReject(message, "Invalid message format");
                    return;
                }
                
                // Check sequence number
                if (validateSequenceNumbers && !isValidSequenceNumber(message)) {
                    log.warn("Invalid sequence number in session {}: expected {}, got {}", 
                            sessionId, nextIncomingSeqNum.get(), message.getMessageSequenceNumber());
                    handleSequenceNumberGap(message);
                    return;
                }
                
                // Store incoming message (if message store is available)
                if (messageStore != null) {
                    messageStore.storeMessage(sessionId, message, MessageStore.MessageDirection.INCOMING);
                } else {
                    log.debug("MessageStore not available - message not persisted");
                }
                
                // Update sequence number
                if (validateSequenceNumbers) {
                    nextIncomingSeqNum.incrementAndGet();
                }
                
                // Update heartbeat timestamp
                lastHeartbeat.set(LocalDateTime.now());
                
                // Process message by type
                processMessageByType(message);
                
            } catch (Exception e) {
                log.error("Error processing message in session {}: {}", sessionId, e.getMessage(), e);
                sendReject(message, "Internal processing error");
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> sendMessage(FIXMessage message) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Set session-specific fields
                message.setField(FIXMessage.SENDER_COMP_ID, senderCompId);
                message.setField(FIXMessage.TARGET_COMP_ID, targetCompId);
                message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(nextOutgoingSeqNum.get()));
                message.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")));
                
                // Store outgoing message (if message store is available)
                if (messageStore != null) {
                    messageStore.storeMessage(sessionId, message, MessageStore.MessageDirection.OUTGOING);
                } else {
                    log.debug("MessageStore not available - message not persisted");
                }
                
                // Increment sequence number
                nextOutgoingSeqNum.incrementAndGet();
                
                // Format and send message (actual network sending would be handled by connection layer)
                String formattedMessage = protocolHandler.format(message);
                log.debug("Sending message from session {}: {}", sessionId, formattedMessage);
                
                // In a real implementation, this would send to the network connection
                // For now, we just log it
                
            } catch (Exception e) {
                log.error("Error sending message from session {}: {}", sessionId, e.getMessage(), e);
                throw new RuntimeException("Failed to send message", e);
            }
        });
    }
    
    @Override
    public void handleHeartbeat() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastHb = lastHeartbeat.get();
            
            if (lastHb != null && now.minusSeconds(heartbeatInterval).isAfter(lastHb)) {
                log.debug("Sending heartbeat for session {}", sessionId);
                
                FIXMessage heartbeat = createHeartbeat();
                sendMessage(heartbeat);
                lastHeartbeat.set(now);
            }
        } catch (Exception e) {
            log.error("Error handling heartbeat for session {}: {}", sessionId, e.getMessage(), e);
        }
    }
    
    /**
     * Send a test request message
     */
    public CompletableFuture<Void> sendTestRequest(String testReqId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Sending test request for session {}: {}", sessionId, testReqId);
                
                FIXMessage testRequest = new FIXMessageImpl("FIX.4.4", "1"); // Test Request
                testRequest.setField(112, testReqId); // TestReqID
                
                sendMessage(testRequest).join();
            } catch (Exception e) {
                log.error("Error sending test request for session {}: {}", sessionId, e.getMessage(), e);
                throw new RuntimeException("Failed to send test request", e);
            }
        });
    }
    
    /**
     * Check if session needs heartbeat based on last activity
     */
    public boolean needsHeartbeat() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastHb = lastHeartbeat.get();
        
        return lastHb != null && now.minusSeconds(heartbeatInterval).isAfter(lastHb);
    }
    
    /**
     * Check if session has timed out
     */
    public boolean hasTimedOut() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastHb = lastHeartbeat.get();
        
        // Consider timed out if no heartbeat for 2x heartbeat interval
        return lastHb != null && now.minusSeconds(heartbeatInterval * 2L).isAfter(lastHb);
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Disconnecting session {}", sessionId);
                
                if (status.get() == Status.LOGGED_ON) {
                    // Send logout message
                    FIXMessage logout = createLogout("User requested disconnect");
                    sendMessage(logout).join();
                    status.set(Status.LOGOUT_SENT);
                }
                
                status.set(Status.DISCONNECTED);
                log.info("Session {} disconnected", sessionId);
                
            } catch (Exception e) {
                log.error("Error disconnecting session {}: {}", sessionId, e.getMessage(), e);
                status.set(Status.DISCONNECTED);
            }
        });
    }
    
    @Override
    public boolean isActive() {
        Status currentStatus = status.get();
        return currentStatus == Status.LOGGED_ON || currentStatus == Status.CONNECTING;
    }
    
    /**
     * Initiate session logon
     */
    public CompletableFuture<Void> logon(int heartbeatInterval, String encryptMethod) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Initiating logon for session {}", sessionId);
                
                status.set(Status.CONNECTING);
                sessionStartTime.set(LocalDateTime.now());
                
                FIXMessage logonMessage = createLogon(heartbeatInterval, encryptMethod);
                sendMessage(logonMessage).join();
                
                status.set(Status.LOGON_SENT);
                log.info("Logon sent for session {}", sessionId);
                
            } catch (Exception e) {
                log.error("Error during logon for session {}: {}", sessionId, e.getMessage(), e);
                status.set(Status.DISCONNECTED);
                throw new RuntimeException("Logon failed", e);
            }
        });
    }
    
    /**
     * Reset sequence numbers (typically used during logon with ResetSeqNumFlag)
     */
    public void resetSequenceNumbers() {
        log.info("Resetting sequence numbers for session {}", sessionId);
        nextIncomingSeqNum.set(1);
        nextOutgoingSeqNum.set(1);
    }
    
    private boolean isValidMessage(FIXMessage message) {
        if (message == null) {
            return false;
        }
        
        // Basic validation
        if (message.getBeginString() == null || message.getMessageType() == null) {
            return false;
        }
        
        // Check sender/target
        if (!targetCompId.equals(message.getSenderCompId()) || 
            !senderCompId.equals(message.getTargetCompId())) {
            log.warn("Message sender/target mismatch in session {}: expected {}->{}. got {}->{}",
                    sessionId, targetCompId, senderCompId, 
                    message.getSenderCompId(), message.getTargetCompId());
            return false;
        }
        
        return true;
    }
    
    private boolean isValidSequenceNumber(FIXMessage message) {
        int expectedSeqNum = nextIncomingSeqNum.get();
        int actualSeqNum = message.getMessageSequenceNumber();
        
        return actualSeqNum == expectedSeqNum;
    }
    
    private void handleSequenceNumberGap(FIXMessage message) {
        int expectedSeqNum = nextIncomingSeqNum.get();
        int actualSeqNum = message.getMessageSequenceNumber();
        
        if (actualSeqNum > expectedSeqNum) {
            // Gap detected - request resend
            log.info("Sequence gap detected in session {}: expected {}, got {}. Requesting resend.",
                    sessionId, expectedSeqNum, actualSeqNum);
            
            FIXMessage resendRequest = createResendRequest(expectedSeqNum, actualSeqNum - 1);
            sendMessage(resendRequest);
        } else if (actualSeqNum < expectedSeqNum) {
            // Duplicate or old message
            log.warn("Received old message in session {}: expected {}, got {}",
                    sessionId, expectedSeqNum, actualSeqNum);
            // Could send reject or ignore based on configuration
        }
    }
    
    private void processMessageByType(FIXMessage message) {
        String msgType = message.getMessageType();
        MessageType messageType = MessageType.fromValue(msgType);
        
        if (messageType == null) {
            log.warn("Unknown message type {} in session {}", msgType, sessionId);
            sendReject(message, "Unknown message type");
            return;
        }
        
        switch (messageType) {
            case LOGON:
                handleLogon(message);
                break;
                
            case LOGOUT:
                handleLogout(message);
                break;
                
            case HEARTBEAT:
                handleHeartbeatMessage(message);
                break;
                
            case TEST_REQUEST:
                handleTestRequest(message);
                break;
                
            case RESEND_REQUEST:
                handleResendRequest(message);
                break;
                
            case SEQUENCE_RESET:
                handleSequenceReset(message);
                break;
                
            default:
                // Application-level message
                handleApplicationMessage(message);
                break;
        }
    }
    
    private void handleLogon(FIXMessage message) {
        log.info("Received logon message in session {}", sessionId);
        
        // Extract heartbeat interval
        String heartbeatStr = message.getField(108); // HeartBtInt
        if (heartbeatStr != null) {
            try {
                int clientHeartbeat = Integer.parseInt(heartbeatStr);
                log.debug("Client requested heartbeat interval: {} seconds", clientHeartbeat);
            } catch (NumberFormatException e) {
                log.warn("Invalid heartbeat interval in logon: {}", heartbeatStr);
            }
        }
        
        // Check for reset sequence number flag
        String resetFlag = message.getField(141); // ResetSeqNumFlag
        if ("Y".equals(resetFlag)) {
            resetSequenceNumbers();
        }
        
        status.set(Status.LOGGED_ON);
        log.info("Session {} is now logged on", sessionId);
        
        // Send logon response if we're the acceptor
        // (In a real implementation, this would depend on session role)
    }
    
    private void handleLogout(FIXMessage message) {
        log.info("Received logout message in session {}", sessionId);
        
        String text = message.getField(58); // Text
        if (text != null) {
            log.info("Logout reason: {}", text);
        }
        
        status.set(Status.DISCONNECTED);
        
        // Send logout acknowledgment
        FIXMessage logoutAck = createLogout("Logout acknowledged");
        sendMessage(logoutAck);
    }
    
    private void handleHeartbeatMessage(FIXMessage message) {
        log.debug("Received heartbeat in session {}", sessionId);
        // Heartbeat received - no action needed, timestamp already updated
    }
    
    private void handleTestRequest(FIXMessage message) {
        String testReqId = message.getField(112); // TestReqID
        log.debug("Received test request in session {}: {}", sessionId, testReqId);
        
        // Send heartbeat with TestReqID
        FIXMessage heartbeat = createHeartbeat();
        if (testReqId != null) {
            heartbeat.setField(112, testReqId); // Echo TestReqID
        }
        sendMessage(heartbeat);
    }
    
    private void handleResendRequest(FIXMessage message) {
        String beginSeqNoStr = message.getField(7); // BeginSeqNo
        String endSeqNoStr = message.getField(16); // EndSeqNo
        
        log.info("Received resend request in session {}: {} to {}", sessionId, beginSeqNoStr, endSeqNoStr);
        
        try {
            int beginSeqNo = Integer.parseInt(beginSeqNoStr);
            int endSeqNo = "0".equals(endSeqNoStr) ? Integer.MAX_VALUE : Integer.parseInt(endSeqNoStr);
            
            // Retrieve and resend messages (if message store is available)
            List<FIXMessage> messagesToResend = messageStore != null ? 
                messageStore.getMessages(sessionId, beginSeqNo, endSeqNo) : 
                new java.util.ArrayList<>();
            for (FIXMessage msg : messagesToResend) {
                // Mark as possible duplicate
                msg.setField(43, "Y"); // PossDupFlag
                // Update sending time
                msg.setField(FIXMessage.SENDING_TIME, LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss")));
                sendMessage(msg);
            }
            
        } catch (NumberFormatException e) {
            log.error("Invalid sequence numbers in resend request: {} to {}", beginSeqNoStr, endSeqNoStr);
            sendReject(message, "Invalid sequence numbers");
        }
    }
    
    private void handleSequenceReset(FIXMessage message) {
        String newSeqNoStr = message.getField(36); // NewSeqNo
        String gapFillFlag = message.getField(123); // GapFillFlag
        
        log.info("Received sequence reset in session {}: new seq {}, gap fill: {}", 
                sessionId, newSeqNoStr, gapFillFlag);
        
        try {
            int newSeqNo = Integer.parseInt(newSeqNoStr);
            nextIncomingSeqNum.set(newSeqNo);
            
            if (!"Y".equals(gapFillFlag)) {
                // Hard reset - reset our outgoing sequence too
                log.info("Hard sequence reset for session {}", sessionId);
            }
            
        } catch (NumberFormatException e) {
            log.error("Invalid new sequence number in sequence reset: {}", newSeqNoStr);
            sendReject(message, "Invalid new sequence number");
        }
    }
    
    private void handleApplicationMessage(FIXMessage message) {
        log.debug("Received application message in session {}: {}", sessionId, message.getMessageType());
        // Application-specific processing would go here
        // For now, just log it
    }
    
    private void sendReject(FIXMessage originalMessage, String reason) {
        try {
            FIXMessage reject = new FIXMessageImpl("FIX.4.4", "3"); // Reject
            reject.setField(45, String.valueOf(originalMessage.getMessageSequenceNumber())); // RefSeqNum
            reject.setField(58, reason); // Text
            reject.setField(371, "0"); // RefTagID (0 = message level reject)
            reject.setField(372, "5"); // RefMsgType
            reject.setField(373, "5"); // SessionRejectReason (Other)
            
            sendMessage(reject);
        } catch (Exception e) {
            log.error("Failed to send reject message: {}", e.getMessage(), e);
        }
    }
    
    private FIXMessage createLogon(int heartbeatInterval, String encryptMethod) {
        FIXMessage logon = new FIXMessageImpl("FIX.4.4", "A"); // Logon
        logon.setField(98, encryptMethod != null ? encryptMethod : "0"); // EncryptMethod
        logon.setField(108, String.valueOf(heartbeatInterval)); // HeartBtInt
        return logon;
    }
    
    private FIXMessage createLogout(String reason) {
        FIXMessage logout = new FIXMessageImpl("FIX.4.4", "5"); // Logout
        if (reason != null) {
            logout.setField(58, reason); // Text
        }
        return logout;
    }
    
    private FIXMessage createHeartbeat() {
        return new FIXMessageImpl("FIX.4.4", "0"); // Heartbeat
    }
    
    private FIXMessage createResendRequest(int beginSeqNo, int endSeqNo) {
        FIXMessage resendRequest = new FIXMessageImpl("FIX.4.4", "2"); // Resend Request
        resendRequest.setField(7, String.valueOf(beginSeqNo)); // BeginSeqNo
        resendRequest.setField(16, String.valueOf(endSeqNo)); // EndSeqNo
        return resendRequest;
    }
}