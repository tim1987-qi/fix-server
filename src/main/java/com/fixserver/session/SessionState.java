package com.fixserver.session;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Represents the state of a FIX session
 */
public class SessionState {
    
    private final String sessionId;
    private final String senderCompId;
    private final String targetCompId;
    private FIXSession.Status status;
    private int incomingSequenceNumber;
    private int outgoingSequenceNumber;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime sessionStartTime;
    private Duration heartbeatInterval;
    private String lastError;
    private long totalMessagesReceived;
    private long totalMessagesSent;
    
    public SessionState(String sessionId, String senderCompId, String targetCompId) {
        this.sessionId = sessionId;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.status = FIXSession.Status.DISCONNECTED;
        this.incomingSequenceNumber = 1;
        this.outgoingSequenceNumber = 1;
        this.lastHeartbeat = LocalDateTime.now();
        this.heartbeatInterval = Duration.ofSeconds(30);
        this.totalMessagesReceived = 0;
        this.totalMessagesSent = 0;
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getSenderCompId() { return senderCompId; }
    public String getTargetCompId() { return targetCompId; }
    public FIXSession.Status getStatus() { return status; }
    public int getIncomingSequenceNumber() { return incomingSequenceNumber; }
    public int getOutgoingSequenceNumber() { return outgoingSequenceNumber; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public LocalDateTime getSessionStartTime() { return sessionStartTime; }
    public Duration getHeartbeatInterval() { return heartbeatInterval; }
    public String getLastError() { return lastError; }
    public long getTotalMessagesReceived() { return totalMessagesReceived; }
    public long getTotalMessagesSent() { return totalMessagesSent; }
    
    // Setters
    public void setStatus(FIXSession.Status status) { this.status = status; }
    public void setIncomingSequenceNumber(int incomingSequenceNumber) { this.incomingSequenceNumber = incomingSequenceNumber; }
    public void setOutgoingSequenceNumber(int outgoingSequenceNumber) { this.outgoingSequenceNumber = outgoingSequenceNumber; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public void setSessionStartTime(LocalDateTime sessionStartTime) { this.sessionStartTime = sessionStartTime; }
    public void setHeartbeatInterval(Duration heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    // Utility methods
    public void incrementIncomingSequenceNumber() {
        this.incomingSequenceNumber++;
        this.totalMessagesReceived++;
    }
    
    public void incrementOutgoingSequenceNumber() {
        this.outgoingSequenceNumber++;
        this.totalMessagesSent++;
    }
    
    public void resetSequenceNumbers() {
        this.incomingSequenceNumber = 1;
        this.outgoingSequenceNumber = 1;
    }
    
    public boolean isHeartbeatDue() {
        if (lastHeartbeat == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastHeartbeat.plus(heartbeatInterval));
    }
    
    public boolean isTimedOut() {
        if (lastHeartbeat == null) {
            return false;
        }
        // Consider session timed out if no heartbeat for 2x heartbeat interval
        Duration timeoutThreshold = heartbeatInterval.multipliedBy(2);
        return LocalDateTime.now().isAfter(lastHeartbeat.plus(timeoutThreshold));
    }
    
    public Duration getTimeSinceLastHeartbeat() {
        if (lastHeartbeat == null) {
            return Duration.ZERO;
        }
        return Duration.between(lastHeartbeat, LocalDateTime.now());
    }
    
    public Duration getSessionDuration() {
        if (sessionStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(sessionStartTime, LocalDateTime.now());
    }
    
    public double getMessageRate() {
        Duration sessionDuration = getSessionDuration();
        if (sessionDuration.isZero()) {
            return 0.0;
        }
        
        long totalMessages = totalMessagesReceived + totalMessagesSent;
        return totalMessages / (double) sessionDuration.getSeconds();
    }
    
    @Override
    public String toString() {
        return String.format("SessionState{id='%s', status=%s, in=%d, out=%d, lastHB=%s}", 
                sessionId, status, incomingSequenceNumber, outgoingSequenceNumber, lastHeartbeat);
    }
}