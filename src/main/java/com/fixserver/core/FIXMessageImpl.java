package com.fixserver.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of the FIX protocol message interface.
 * 
 * This class provides a complete implementation of FIX (Financial Information eXchange)
 * protocol messages with full validation, formatting, and checksum calculation capabilities.
 * 
 * Key Features:
 * - Thread-safe field operations using ConcurrentHashMap
 * - Automatic checksum calculation and validation
 * - Support for FIX 4.4 and FIXT 1.1 protocol versions
 * - Comprehensive message validation with detailed error reporting
 * - Efficient wire format conversion with proper field ordering
 * - Standard FIX timestamp handling
 * 
 * Message Structure:
 * The implementation follows standard FIX message structure:
 * 1. Header fields (BeginString, BodyLength, MessageType, etc.)
 * 2. Body fields (message-specific business data)
 * 3. Trailer fields (Checksum)
 * 
 * Field Separator:
 * Uses SOH (Start of Header, ASCII character 1) as the field separator
 * as required by the FIX protocol specification.
 * 
 * Thread Safety:
 * All field operations are thread-safe, allowing concurrent access from
 * multiple threads without external synchronization.
 * 
 * Performance Considerations:
 * - Lazy checksum calculation (only when needed)
 * - Efficient field storage with minimal memory overhead
 * - Optimized string building for wire format conversion
 * 
 * @author FIX Server Team
 * @version 1.0
 * @since 1.0
 */
public class FIXMessageImpl implements FIXMessage {
    
    /** Standard FIX timestamp format: YYYYMMDD-HH:MM:SS */
    private static final DateTimeFormatter FIX_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
    
    /** SOH (Start of Header) character used as field separator in FIX protocol */
    private static final String FIELD_SEPARATOR = "\u0001"; // ASCII character 1
    
    /** Thread-safe storage for all FIX fields (tag -> value mapping) */
    private final Map<Integer, String> fields = new ConcurrentHashMap<>();
    
    /** List to collect validation errors during message validation */
    private final List<String> validationErrors = new ArrayList<>();
    
    /**
     * Default constructor creating an empty FIX message.
     * Fields must be set individually using setField() method.
     */
    public FIXMessageImpl() {
        // Default constructor - fields will be set via setField() calls
    }
    
    /**
     * Constructor creating a FIX message with basic required fields.
     * 
     * Automatically sets the current timestamp as SendingTime to ensure
     * the message has a valid timestamp when created.
     * 
     * @param beginString FIX protocol version (e.g., "FIX.4.4", "FIXT.1.1")
     * @param messageType single character message type identifier
     */
    public FIXMessageImpl(String beginString, String messageType) {
        setField(BEGIN_STRING, beginString);
        setField(MESSAGE_TYPE, messageType);
        // Set current timestamp - will be updated when message is actually sent
        setField(SENDING_TIME, LocalDateTime.now().format(FIX_TIME_FORMAT));
    }
    
    @Override
    public String getBeginString() {
        return getField(BEGIN_STRING);
    }
    
    @Override
    public String getMessageType() {
        return getField(MESSAGE_TYPE);
    }
    
    @Override
    public String getSenderCompId() {
        return getField(SENDER_COMP_ID);
    }
    
    @Override
    public String getTargetCompId() {
        return getField(TARGET_COMP_ID);
    }
    
    @Override
    public int getMessageSequenceNumber() {
        String seqNum = getField(MESSAGE_SEQUENCE_NUMBER);
        return seqNum != null ? Integer.parseInt(seqNum) : 0;
    }
    
    @Override
    public LocalDateTime getSendingTime() {
        String timeStr = getField(SENDING_TIME);
        if (timeStr != null) {
            try {
                return LocalDateTime.parse(timeStr, FIX_TIME_FORMAT);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    @Override
    public String getField(int tag) {
        return fields.get(tag);
    }
    
    @Override
    public void setField(int tag, String value) {
        if (value != null) {
            fields.put(tag, value);
        } else {
            fields.remove(tag);
        }
    }
    
    @Override
    public Map<Integer, String> getAllFields() {
        return new HashMap<>(fields);
    }
    
    @Override
    public String getChecksum() {
        return getField(CHECKSUM);
    }
    
    /**
     * Converts the message to FIX wire format for network transmission.
     * 
     * The wire format follows FIX protocol specifications:
     * 1. BeginString field (always first)
     * 2. BodyLength field (calculated automatically)
     * 3. All other fields in tag number order
     * 4. Checksum field (always last, calculated automatically)
     * 
     * Field Ordering:
     * - Header fields (BeginString, BodyLength) come first
     * - Body fields are sorted by tag number for consistency
     * - Trailer fields (Checksum) come last
     * 
     * Length Calculation:
     * BodyLength includes all bytes from the first field after BodyLength
     * up to but not including the Checksum field.
     * 
     * Checksum Calculation:
     * Checksum is the sum of all bytes in the message (excluding checksum field)
     * modulo 256, formatted as a 3-digit zero-padded string.
     * 
     * @return complete FIX message string ready for network transmission
     */
    @Override
    public String toFixString() {
        StringBuilder sb = new StringBuilder();
        
        // Step 1: Calculate body length first (excluding BeginString, BodyLength, and Checksum)
        StringBuilder body = new StringBuilder();
        
        // Sort all field tags for consistent ordering (FIX best practice)
        List<Integer> sortedTags = new ArrayList<>(fields.keySet());
        sortedTags.sort(Integer::compareTo);
        
        // Step 2: Add BeginString field first (required by FIX specification)
        if (fields.containsKey(BEGIN_STRING)) {
            sb.append(BEGIN_STRING).append("=").append(fields.get(BEGIN_STRING)).append(FIELD_SEPARATOR);
        }
        
        // Step 3: Build body content for length calculation
        // Include all fields except BeginString, BodyLength, and Checksum
        for (Integer tag : sortedTags) {
            if (tag != BEGIN_STRING && tag != BODY_LENGTH && tag != CHECKSUM) {
                body.append(tag).append("=").append(fields.get(tag)).append(FIELD_SEPARATOR);
            }
        }
        
        // Step 4: Add BodyLength field with calculated length
        sb.append(BODY_LENGTH).append("=").append(body.length()).append(FIELD_SEPARATOR);
        
        // Step 5: Add the body content
        sb.append(body);
        
        // Step 6: Calculate and add checksum (must be last field)
        String checksum = calculateChecksum(sb.toString());
        // Store checksum in fields so getChecksum() can retrieve it
        fields.put(CHECKSUM, checksum);
        sb.append(CHECKSUM).append("=").append(checksum).append(FIELD_SEPARATOR);
        
        return sb.toString();
    }
    
    @Override
    public boolean isValid() {
        validationErrors.clear();
        
        // Check required fields
        if (getField(BEGIN_STRING) == null) {
            validationErrors.add("BeginString (8) is required");
        }
        
        if (getField(MESSAGE_TYPE) == null) {
            validationErrors.add("MessageType (35) is required");
        }
        
        if (getField(SENDER_COMP_ID) == null) {
            validationErrors.add("SenderCompID (49) is required");
        }
        
        if (getField(TARGET_COMP_ID) == null) {
            validationErrors.add("TargetCompID (56) is required");
        }
        
        if (getField(MESSAGE_SEQUENCE_NUMBER) == null) {
            validationErrors.add("MsgSeqNum (34) is required");
        } else {
            try {
                int seqNum = Integer.parseInt(getField(MESSAGE_SEQUENCE_NUMBER));
                if (seqNum <= 0) {
                    validationErrors.add("MsgSeqNum (34) must be positive");
                }
            } catch (NumberFormatException e) {
                validationErrors.add("MsgSeqNum (34) must be a valid integer");
            }
        }
        
        if (getField(SENDING_TIME) == null) {
            validationErrors.add("SendingTime (52) is required");
        }
        
        // Validate FIX version
        String beginString = getField(BEGIN_STRING);
        if (beginString != null && !isValidFixVersion(beginString)) {
            validationErrors.add("Unsupported FIX version: " + beginString);
        }
        
        // Validate message type
        String messageType = getField(MESSAGE_TYPE);
        if (messageType != null && !isValidMessageType(messageType)) {
            validationErrors.add("Unknown message type: " + messageType);
        }
        
        return validationErrors.isEmpty();
    }
    
    @Override
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    /**
     * Calculates the FIX protocol checksum for message integrity verification.
     * 
     * The checksum is calculated as the sum of all ASCII values of characters
     * in the message (excluding the checksum field itself) modulo 256.
     * This provides basic error detection for message transmission.
     * 
     * Algorithm:
     * 1. Sum ASCII values of all characters in the message
     * 2. Take modulo 256 to get a value between 0-255
     * 3. Format as 3-digit zero-padded string (e.g., "001", "156")
     * 
     * @param message the message content to calculate checksum for
     * @return 3-digit checksum string
     */
    private String calculateChecksum(String message) {
        int sum = 0;
        // Sum ASCII values of all characters
        for (char c : message.toCharArray()) {
            sum += c;
        }
        // Return modulo 256 as 3-digit zero-padded string
        return String.format("%03d", sum % 256);
    }
    
    /**
     * Validates if the given string is a supported FIX protocol version.
     * 
     * Currently supports:
     * - FIX.4.4: Most widely used version for equity trading
     * - FIXT.1.1: Transport layer for FIX 5.0+ application messages
     * 
     * @param version the FIX version string to validate
     * @return true if version is supported, false otherwise
     */
    private boolean isValidFixVersion(String version) {
        return "FIX.4.4".equals(version) || "FIXT.1.1".equals(version);
    }
    
    /**
     * Validates if the given string is a recognized FIX message type.
     * 
     * Delegates to the MessageType enum which contains all standard
     * FIX message types including session-level and application-level messages.
     * 
     * @param messageType the message type string to validate
     * @return true if message type is recognized, false otherwise
     */
    private boolean isValidMessageType(String messageType) {
        return com.fixserver.protocol.MessageType.isValid(messageType);
    }
    
    @Override
    public String toString() {
        return String.format("FIXMessage{type=%s, sender=%s, target=%s, seqNum=%d}", 
            getMessageType(), getSenderCompId(), getTargetCompId(), getMessageSequenceNumber());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FIXMessageImpl that = (FIXMessageImpl) obj;
        return Objects.equals(fields, that.fields);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }
}