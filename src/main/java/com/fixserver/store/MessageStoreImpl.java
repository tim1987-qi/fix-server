package com.fixserver.store;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.store.entity.AuditRecordEntity;
import com.fixserver.store.entity.MessageEntity;
import com.fixserver.store.repository.AuditRepository;
import com.fixserver.store.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of MessageStore using JPA repositories
 */
@Slf4j
@Repository
@Transactional
@ConditionalOnProperty(name = "fix.server.database.enabled", havingValue = "true")
public class MessageStoreImpl implements MessageStore {
    
    private final MessageRepository messageRepository;
    private final AuditRepository auditRepository;
    private final FIXProtocolHandler protocolHandler;
    
    @Autowired
    public MessageStoreImpl(MessageRepository messageRepository, 
                           AuditRepository auditRepository,
                           FIXProtocolHandler protocolHandler) {
        this.messageRepository = messageRepository;
        this.auditRepository = auditRepository;
        this.protocolHandler = protocolHandler;
    }
    
    @Override
    public void storeMessage(String sessionId, FIXMessage message, MessageDirection direction) {
        try {
            String rawMessage = protocolHandler.format(message);
            
            MessageEntity entity = new MessageEntity(
                sessionId,
                message.getMessageSequenceNumber(),
                direction,
                message.getMessageType(),
                message.getSenderCompId(),
                message.getTargetCompId(),
                rawMessage,
                null // Client IP would be set by connection layer
            );
            
            messageRepository.save(entity);
            
            // Create audit record
            AuditRecordEntity auditRecord = new AuditRecordEntity(
                sessionId,
                direction == MessageDirection.INCOMING ? 
                    AuditRecordEntity.AuditEventType.MESSAGE_RECEIVED :
                    AuditRecordEntity.AuditEventType.MESSAGE_SENT,
                message.getMessageType(),
                rawMessage,
                direction,
                null,
                String.format("Message %s: %s", 
                    direction == MessageDirection.INCOMING ? "received" : "sent",
                    message.getMessageType())
            );
            
            auditRepository.save(auditRecord);
            
            log.debug("Stored {} message for session {}: seq={}, type={}", 
                     direction, sessionId, message.getMessageSequenceNumber(), message.getMessageType());
            
        } catch (Exception e) {
            log.error("Failed to store message for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to store message", e);
        }
    }
    
    @Override
    public List<FIXMessage> getMessages(String sessionId, int fromSeqNum, int toSeqNum) {
        try {
            List<MessageEntity> entities = messageRepository.findBySessionIdAndSequenceNumberRange(
                sessionId, MessageDirection.OUTGOING, fromSeqNum, toSeqNum);
            
            return entities.stream()
                .map(this::convertToFIXMessage)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to retrieve messages for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to retrieve messages", e);
        }
    }
    
    @Override
    public Optional<FIXMessage> getMessage(String sessionId, int sequenceNumber, MessageDirection direction) {
        try {
            Optional<MessageEntity> entity = messageRepository.findBySessionIdAndSequenceNumberAndDirection(
                sessionId, sequenceNumber, direction);
            
            return entity.map(this::convertToFIXMessage);
            
        } catch (Exception e) {
            log.error("Failed to retrieve message for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to retrieve message", e);
        }
    }
    
    @Override
    public int getLastSequenceNumber(String sessionId, MessageDirection direction) {
        try {
            Integer maxSeqNum = messageRepository.findMaxSequenceNumber(sessionId, direction);
            return maxSeqNum != null ? maxSeqNum : 0;
            
        } catch (Exception e) {
            log.error("Failed to get last sequence number for session {}: {}", sessionId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public void archiveMessages(String sessionId, LocalDateTime beforeDate) {
        try {
            int archivedCount = messageRepository.markMessagesAsArchived(
                sessionId, beforeDate, LocalDateTime.now());
            
            log.info("Archived {} messages for session {} before {}", 
                    archivedCount, sessionId, beforeDate);
            
            // Create audit record
            AuditRecordEntity auditRecord = new AuditRecordEntity(
                sessionId,
                AuditRecordEntity.AuditEventType.SYSTEM_ERROR, // Using SYSTEM_ERROR as generic system event
                String.format("Archived %d messages before %s", archivedCount, beforeDate)
            );
            auditRepository.save(auditRecord);
            
        } catch (Exception e) {
            log.error("Failed to archive messages for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to archive messages", e);
        }
    }
    
    @Override
    public List<AuditRecord> getAuditTrail(String sessionId, LocalDateTime from, LocalDateTime to) {
        try {
            List<AuditRecordEntity> entities = auditRepository.findBySessionIdAndTimestampRange(
                sessionId, from, to);
            
            return entities.stream()
                .map(entity -> (AuditRecord) entity)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to retrieve audit trail for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to retrieve audit trail", e);
        }
    }
    
    @Override
    public List<String> getActiveSessions() {
        try {
            return messageRepository.findDistinctSessionIds();
        } catch (Exception e) {
            log.error("Failed to retrieve active sessions: {}", e.getMessage(), e);
            throw new MessageStoreException("Failed to retrieve active sessions", e);
        }
    }
    
    @Override
    public void clearSession(String sessionId) {
        try {
            List<MessageEntity> messages = messageRepository.findRecentMessagesBySessionId(sessionId);
            messageRepository.deleteAll(messages);
            
            List<AuditRecordEntity> auditRecords = auditRepository.findBySessionIdOrderByTimestampDesc(sessionId);
            auditRepository.deleteAll(auditRecords);
            
            log.warn("Cleared all messages and audit records for session {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to clear session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to clear session", e);
        }
    }
    
    /**
     * Get message statistics for a session
     */
    public MessageStatistics getMessageStatistics(String sessionId) {
        try {
            long totalMessages = messageRepository.countBySessionId(sessionId);
            long incomingMessages = messageRepository.countBySessionIdAndDirection(
                sessionId, MessageDirection.INCOMING);
            long outgoingMessages = messageRepository.countBySessionIdAndDirection(
                sessionId, MessageDirection.OUTGOING);
            
            return new MessageStatistics(sessionId, totalMessages, incomingMessages, outgoingMessages);
            
        } catch (Exception e) {
            log.error("Failed to get message statistics for session {}: {}", sessionId, e.getMessage(), e);
            throw new MessageStoreException("Failed to get message statistics", e);
        }
    }
    
    /**
     * Cleanup old archived messages
     */
    public void cleanupArchivedMessages(LocalDateTime beforeDate) {
        try {
            messageRepository.deleteArchivedMessagesBefore(beforeDate);
            auditRepository.deleteAuditRecordsBefore(beforeDate);
            
            log.info("Cleaned up archived messages and audit records before {}", beforeDate);
            
        } catch (Exception e) {
            log.error("Failed to cleanup archived messages: {}", e.getMessage(), e);
            throw new MessageStoreException("Failed to cleanup archived messages", e);
        }
    }
    
    /**
     * Create audit record for session events
     */
    public void createAuditRecord(String sessionId, AuditRecordEntity.AuditEventType eventType, 
                                 String description) {
        try {
            AuditRecordEntity auditRecord = new AuditRecordEntity(sessionId, eventType, description);
            auditRepository.save(auditRecord);
            
            log.debug("Created audit record for session {}: {} - {}", sessionId, eventType, description);
            
        } catch (Exception e) {
            log.error("Failed to create audit record for session {}: {}", sessionId, e.getMessage(), e);
            // Don't throw exception for audit failures to avoid disrupting main flow
        }
    }
    
    private FIXMessage convertToFIXMessage(MessageEntity entity) {
        try {
            return protocolHandler.parse(entity.getRawMessage());
        } catch (Exception e) {
            log.error("Failed to parse stored message: {}", e.getMessage(), e);
            // Return a basic message with available information
            FIXMessageImpl message = new FIXMessageImpl();
            message.setField(FIXMessage.MESSAGE_TYPE, entity.getMessageType());
            message.setField(FIXMessage.SENDER_COMP_ID, entity.getSenderCompId());
            message.setField(FIXMessage.TARGET_COMP_ID, entity.getTargetCompId());
            message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(entity.getSequenceNumber()));
            return message;
        }
    }
    
    /**
     * Message statistics container
     */
    public static class MessageStatistics {
        private final String sessionId;
        private final long totalMessages;
        private final long incomingMessages;
        private final long outgoingMessages;
        
        public MessageStatistics(String sessionId, long totalMessages, 
                               long incomingMessages, long outgoingMessages) {
            this.sessionId = sessionId;
            this.totalMessages = totalMessages;
            this.incomingMessages = incomingMessages;
            this.outgoingMessages = outgoingMessages;
        }
        
        public String getSessionId() { return sessionId; }
        public long getTotalMessages() { return totalMessages; }
        public long getIncomingMessages() { return incomingMessages; }
        public long getOutgoingMessages() { return outgoingMessages; }
    }
    
    /**
     * Exception for message store operations
     */
    public static class MessageStoreException extends RuntimeException {
        public MessageStoreException(String message) {
            super(message);
        }
        
        public MessageStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}