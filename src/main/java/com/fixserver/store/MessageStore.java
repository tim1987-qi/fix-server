package com.fixserver.store;

import com.fixserver.core.FIXMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core interface for persistent storage of FIX messages with comprehensive audit capabilities.
 * 
 * This interface provides the contract for storing, retrieving, and managing FIX messages
 * throughout their lifecycle. It supports:
 * 
 * Key Features:
 * - Persistent message storage with directional tracking (incoming/outgoing)
 * - Message replay functionality for FIX protocol compliance
 * - Comprehensive audit trail for regulatory compliance
 * - Data archival and retention management
 * - Session-based message organization
 * 
 * Compliance Requirements:
 * - All messages must be stored with timestamps for audit trails
 * - Message replay must be available for gap filling and recovery
 * - Audit records must be immutable and traceable
 * - Data retention policies must be configurable
 * 
 * Performance Considerations:
 * - Optimized for high-frequency message storage
 * - Efficient retrieval by sequence number ranges
 * - Background archival to manage storage growth
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface MessageStore {
    
    /**
     * Stores a FIX message persistently with audit trail creation.
     * 
     * This method persists the message and automatically creates an audit record
     * for compliance tracking. The message is stored with its complete context
     * including session information, timestamp, and directional metadata.
     * 
     * @param sessionId unique identifier of the session
     * @param message the FIX message to store
     * @param direction whether message was incoming or outgoing
     * @throws MessageStoreException if storage fails
     */
    void storeMessage(String sessionId, FIXMessage message, MessageDirection direction);
    
    /**
     * Retrieves messages for replay within a sequence number range.
     * 
     * Used primarily for FIX message replay functionality when counterparties
     * request resend of messages due to gaps or connection issues. Messages
     * are returned in sequence number order.
     * 
     * @param sessionId unique identifier of the session
     * @param fromSeqNum starting sequence number (inclusive)
     * @param toSeqNum ending sequence number (inclusive)
     * @return list of messages in sequence order (may be empty)
     */
    List<FIXMessage> getMessages(String sessionId, int fromSeqNum, int toSeqNum);
    
    /**
     * Retrieves a specific message by session, sequence number, and direction.
     * 
     * Used for precise message lookup during replay operations or
     * troubleshooting specific message issues.
     * 
     * @param sessionId unique identifier of the session
     * @param sequenceNumber the message sequence number
     * @param direction whether to look for incoming or outgoing message
     * @return Optional containing the message if found, empty otherwise
     */
    Optional<FIXMessage> getMessage(String sessionId, int sequenceNumber, MessageDirection direction);
    
    /**
     * Gets the highest sequence number used for a session and direction.
     * 
     * Critical for session recovery and determining the next sequence number
     * to use when resuming a session after reconnection.
     * 
     * @param sessionId unique identifier of the session
     * @param direction whether to check incoming or outgoing messages
     * @return highest sequence number used, or 0 if no messages exist
     */
    int getLastSequenceNumber(String sessionId, MessageDirection direction);
    
    /**
     * Archives old messages based on retention policy to manage storage growth.
     * 
     * Moves messages older than the specified date to archived status.
     * Archived messages are typically moved to cheaper storage or compressed
     * but remain available for compliance queries.
     * 
     * @param sessionId unique identifier of the session
     * @param beforeDate archive messages older than this date
     */
    void archiveMessages(String sessionId, LocalDateTime beforeDate);
    
    /**
     * Retrieves comprehensive audit trail for compliance and troubleshooting.
     * 
     * Returns all audit events for a session within the specified time range.
     * Includes message events, session lifecycle events, and error conditions.
     * 
     * @param sessionId unique identifier of the session
     * @param from start of time range (inclusive)
     * @param to end of time range (inclusive)
     * @return list of audit records in chronological order
     */
    List<AuditRecord> getAuditTrail(String sessionId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Gets list of all sessions that have stored messages.
     * 
     * Used for administrative operations, monitoring, and cleanup tasks.
     * Only returns sessions that have at least one stored message.
     * 
     * @return list of session identifiers with stored messages
     */
    List<String> getActiveSessions();
    
    /**
     * Permanently removes all messages and audit records for a session.
     * 
     * WARNING: This operation is irreversible and should only be used
     * for testing or when explicitly required for data privacy compliance.
     * Consider archival instead of deletion for production systems.
     * 
     * @param sessionId unique identifier of the session to clear
     */
    void clearSession(String sessionId);
    
    /**
     * Enumeration indicating the direction of message flow relative to the server.
     * 
     * Used to distinguish between messages received from counterparties and
     * messages sent to counterparties. Critical for proper sequence number
     * management and replay functionality.
     */
    enum MessageDirection {
        /** Message received from a counterparty */
        INCOMING,
        /** Message sent to a counterparty */
        OUTGOING
    }
    
    /**
     * Interface representing an immutable audit record for compliance tracking.
     * 
     * Audit records provide a complete trail of all activities within the system
     * for regulatory compliance, troubleshooting, and forensic analysis. Each
     * record captures the context and details of a specific event.
     * 
     * Audit records are immutable once created and must be retained according
     * to regulatory requirements (typically 7+ years for financial systems).
     */
    interface AuditRecord {
        /** Unique identifier for this audit record */
        Long getId();
        
        /** Session identifier this record relates to */
        String getSessionId();
        
        /** Timestamp when the audited event occurred */
        LocalDateTime getTimestamp();
        
        /** FIX message type if this record relates to a message */
        String getMessageType();
        
        /** Complete raw message content for message-related records */
        String getRawMessage();
        
        /** Direction of message flow if applicable */
        MessageDirection getDirection();
        
        /** IP address of client if applicable for security tracking */
        String getClientIpAddress();
    }
}