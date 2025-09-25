package com.fixserver.store;

import com.fixserver.core.FIXMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of MessageStore for testing and development.
 * 
 * This implementation stores all messages in memory and provides basic
 * functionality without requiring a database. It's suitable for:
 * - Development and testing
 * - Proof of concept deployments
 * - Environments where database setup is not desired
 * 
 * WARNING: All data is lost when the application restarts.
 * For production use, switch to MessageStoreImpl with proper database.
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public class InMemoryMessageStore implements MessageStore {
    
    /** Storage for messages organized by session and direction */
    private final Map<String, Map<MessageDirection, Map<Integer, FIXMessage>>> messageStorage = new ConcurrentHashMap<>();
    
    /** Storage for audit records */
    private final Map<String, List<AuditRecord>> auditStorage = new ConcurrentHashMap<>();
    
    /** Counter for generating audit record IDs */
    private final AtomicLong auditIdCounter = new AtomicLong(1);
    
    @Override
    public void storeMessage(String sessionId, FIXMessage message, MessageDirection direction) {
        log.debug("Storing {} message for session {}: seq={}, type={}", 
                 direction, sessionId, message.getMessageSequenceNumber(), message.getMessageType());
        
        // Get or create session storage
        Map<MessageDirection, Map<Integer, FIXMessage>> sessionStorage = 
            messageStorage.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
        
        // Get or create direction storage
        Map<Integer, FIXMessage> directionStorage = 
            sessionStorage.computeIfAbsent(direction, k -> new ConcurrentHashMap<>());
        
        // Store the message
        directionStorage.put(message.getMessageSequenceNumber(), message);
        
        // Create audit record
        createAuditRecord(sessionId, direction, message);
        
        log.debug("Successfully stored message for session {}", sessionId);
    }
    
    @Override
    public List<FIXMessage> getMessages(String sessionId, int fromSeqNum, int toSeqNum) {
        log.debug("Retrieving messages for session {}: seq {} to {}", sessionId, fromSeqNum, toSeqNum);
        
        Map<MessageDirection, Map<Integer, FIXMessage>> sessionStorage = messageStorage.get(sessionId);
        if (sessionStorage == null) {
            log.debug("No messages found for session {}", sessionId);
            return new ArrayList<>();
        }
        
        Map<Integer, FIXMessage> outgoingMessages = sessionStorage.get(MessageDirection.OUTGOING);
        if (outgoingMessages == null) {
            log.debug("No outgoing messages found for session {}", sessionId);
            return new ArrayList<>();
        }
        
        List<FIXMessage> result = new ArrayList<>();
        for (int seqNum = fromSeqNum; seqNum <= toSeqNum; seqNum++) {
            FIXMessage message = outgoingMessages.get(seqNum);
            if (message != null) {
                result.add(message);
            }
        }
        
        log.debug("Retrieved {} messages for session {}", result.size(), sessionId);
        return result;
    }
    
    @Override
    public Optional<FIXMessage> getMessage(String sessionId, int sequenceNumber, MessageDirection direction) {
        Map<MessageDirection, Map<Integer, FIXMessage>> sessionStorage = messageStorage.get(sessionId);
        if (sessionStorage == null) {
            return Optional.empty();
        }
        
        Map<Integer, FIXMessage> directionStorage = sessionStorage.get(direction);
        if (directionStorage == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(directionStorage.get(sequenceNumber));
    }
    
    @Override
    public int getLastSequenceNumber(String sessionId, MessageDirection direction) {
        Map<MessageDirection, Map<Integer, FIXMessage>> sessionStorage = messageStorage.get(sessionId);
        if (sessionStorage == null) {
            return 0;
        }
        
        Map<Integer, FIXMessage> directionStorage = sessionStorage.get(direction);
        if (directionStorage == null || directionStorage.isEmpty()) {
            return 0;
        }
        
        return directionStorage.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
    
    @Override
    public void archiveMessages(String sessionId, LocalDateTime beforeDate) {
        log.info("Archive operation requested for session {} before {}", sessionId, beforeDate);
        log.warn("In-memory store does not support archiving - messages remain in memory");
        
        // Create audit record for the archive request
        createSimpleAuditRecord(sessionId, "ARCHIVE_REQUESTED", 
            "Archive requested for messages before " + beforeDate + " (not implemented in memory store)");
    }
    
    @Override
    public List<AuditRecord> getAuditTrail(String sessionId, LocalDateTime from, LocalDateTime to) {
        List<AuditRecord> sessionAuditRecords = auditStorage.get(sessionId);
        if (sessionAuditRecords == null) {
            return new ArrayList<>();
        }
        
        return sessionAuditRecords.stream()
            .filter(record -> {
                LocalDateTime timestamp = record.getTimestamp();
                return timestamp != null && 
                       !timestamp.isBefore(from) && 
                       !timestamp.isAfter(to);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getActiveSessions() {
        return new ArrayList<>(messageStorage.keySet());
    }
    
    @Override
    public void clearSession(String sessionId) {
        log.warn("Clearing all data for session {}", sessionId);
        messageStorage.remove(sessionId);
        auditStorage.remove(sessionId);
        log.info("Cleared session {}", sessionId);
    }
    
    /**
     * Get statistics about stored messages (for monitoring)
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", messageStorage.size());
        
        int totalMessages = 0;
        for (Map<MessageDirection, Map<Integer, FIXMessage>> sessionStorage : messageStorage.values()) {
            for (Map<Integer, FIXMessage> directionStorage : sessionStorage.values()) {
                totalMessages += directionStorage.size();
            }
        }
        stats.put("totalMessages", totalMessages);
        
        int totalAuditRecords = auditStorage.values().stream()
            .mapToInt(List::size)
            .sum();
        stats.put("totalAuditRecords", totalAuditRecords);
        
        return stats;
    }
    
    private void createAuditRecord(String sessionId, MessageDirection direction, FIXMessage message) {
        String eventType = direction == MessageDirection.INCOMING ? "MESSAGE_RECEIVED" : "MESSAGE_SENT";
        String description = String.format("Message %s: type=%s, seq=%d", 
            direction.toString().toLowerCase(), 
            message.getMessageType(), 
            message.getMessageSequenceNumber());
        
        createSimpleAuditRecord(sessionId, eventType, description);
    }
    
    private void createSimpleAuditRecord(String sessionId, String eventType, String description) {
        List<AuditRecord> sessionAuditRecords = auditStorage.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        InMemoryAuditRecord auditRecord = new InMemoryAuditRecord(
            auditIdCounter.getAndIncrement(),
            sessionId,
            LocalDateTime.now(),
            eventType,
            description
        );
        
        sessionAuditRecords.add(auditRecord);
    }
    
    /**
     * Simple in-memory implementation of AuditRecord
     */
    private static class InMemoryAuditRecord implements AuditRecord {
        private final Long id;
        private final String sessionId;
        private final LocalDateTime timestamp;
        private final String eventType;
        private final String description;
        
        public InMemoryAuditRecord(Long id, String sessionId, LocalDateTime timestamp, 
                                  String eventType, String description) {
            this.id = id;
            this.sessionId = sessionId;
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.description = description;
        }
        
        @Override
        public Long getId() { return id; }
        
        @Override
        public String getSessionId() { return sessionId; }
        
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String getMessageType() { return null; } // Not stored in simple implementation
        
        @Override
        public String getRawMessage() { return null; } // Not stored in simple implementation
        
        @Override
        public MessageDirection getDirection() { return null; } // Not stored in simple implementation
        
        @Override
        public String getClientIpAddress() { return null; } // Not stored in simple implementation
        
        @Override
        public String toString() {
            return String.format("AuditRecord{id=%d, session=%s, time=%s, event=%s, desc='%s'}", 
                                id, sessionId, timestamp, eventType, description);
        }
    }
}