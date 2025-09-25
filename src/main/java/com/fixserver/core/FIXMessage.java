package com.fixserver.core;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core interface representing a FIX (Financial Information eXchange) protocol message.
 * 
 * This interface defines the contract for all FIX messages in the system, providing
 * access to standard FIX fields and message validation capabilities. FIX messages
 * are the fundamental unit of communication in financial trading systems.
 * 
 * Key Features:
 * - Standard FIX field access (BeginString, MessageType, SenderCompID, etc.)
 * - Message validation according to FIX protocol specifications
 * - Conversion to/from FIX wire format
 * - Thread-safe field operations
 * 
 * FIX Message Structure:
 * - Header: Contains routing and administrative fields
 * - Body: Contains business-specific fields
 * - Trailer: Contains checksum for message integrity
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public interface FIXMessage {
    
    // Standard FIX field tags - These are the official FIX protocol field identifiers
    /** FIX version identifier (e.g., "FIX.4.4") - Always first field in message */
    int BEGIN_STRING = 8;
    /** Length of message body in bytes - Used for message parsing */
    int BODY_LENGTH = 9;
    /** Three-digit checksum for message integrity - Always last field */
    int CHECKSUM = 10;
    /** Single character identifying message type (e.g., "D" for New Order) */
    int MESSAGE_TYPE = 35;
    /** Unique identifier of message sender */
    int SENDER_COMP_ID = 49;
    /** Unique identifier of message recipient */
    int TARGET_COMP_ID = 56;
    /** Sequential message number for ordering and gap detection */
    int MESSAGE_SEQUENCE_NUMBER = 34;
    /** Timestamp when message was sent (format: YYYYMMDD-HH:MM:SS) */
    int SENDING_TIME = 52;
    
    /**
     * Retrieves the FIX protocol version from the BeginString field.
     * This identifies which version of the FIX protocol the message conforms to.
     * 
     * @return FIX version string (e.g., "FIX.4.4", "FIXT.1.1") or null if not set
     */
    String getBeginString();
    
    /**
     * Gets the message type identifier that determines the message's purpose.
     * Common types include: "A" (Logon), "D" (New Order), "8" (Execution Report)
     * 
     * @return single character message type or null if not set
     */
    String getMessageType();
    
    /**
     * Gets the unique identifier of the message sender.
     * This is used for message routing and session identification.
     * 
     * @return sender company ID string or null if not set
     */
    String getSenderCompId();
    
    /**
     * Gets the unique identifier of the intended message recipient.
     * This is used for message routing and session identification.
     * 
     * @return target company ID string or null if not set
     */
    String getTargetCompId();
    
    /**
     * Gets the sequential message number used for ordering and gap detection.
     * Each session maintains separate sequence numbers for incoming and outgoing messages.
     * 
     * @return sequence number (positive integer) or 0 if not set
     */
    int getMessageSequenceNumber();
    
    /**
     * Gets the timestamp when the message was sent.
     * Used for message ordering and timeout detection.
     * 
     * @return sending time as LocalDateTime or null if not set/invalid format
     */
    LocalDateTime getSendingTime();
    
    /**
     * Retrieves the value of a specific FIX field by its tag number.
     * 
     * @param tag the FIX field tag number (e.g., 35 for MessageType)
     * @return field value as string or null if field not present
     */
    String getField(int tag);
    
    /**
     * Sets the value of a FIX field by its tag number.
     * If value is null, the field is removed from the message.
     * 
     * @param tag the FIX field tag number
     * @param value the field value to set (null to remove field)
     */
    void setField(int tag, String value);
    
    /**
     * Returns a copy of all fields in the message as a map.
     * The returned map is a snapshot and modifications won't affect the original message.
     * 
     * @return map of field tag numbers to values
     */
    Map<Integer, String> getAllFields();
    
    /**
     * Gets the calculated checksum for message integrity verification.
     * The checksum is calculated as the sum of all bytes in the message modulo 256.
     * 
     * @return three-digit checksum string or null if not calculated
     */
    String getChecksum();
    
    /**
     * Converts the message to FIX wire format for transmission.
     * The wire format uses SOH (Start of Header, ASCII 1) as field separator.
     * 
     * @return complete FIX message string ready for transmission
     */
    String toFixString();
    
    /**
     * Validates the message according to FIX protocol rules.
     * Checks for required fields, proper formats, and business logic constraints.
     * 
     * @return true if message is valid, false otherwise
     */
    boolean isValid();
    
    /**
     * Gets detailed validation error messages if validation fails.
     * Provides specific information about what makes the message invalid.
     * 
     * @return list of validation error descriptions (empty if valid)
     */
    java.util.List<String> getValidationErrors();
}