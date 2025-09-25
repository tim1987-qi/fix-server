package com.fixserver.protocol;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Handles FIX protocol parsing, formatting, and validation
 */
@Slf4j
@Component
public class FIXProtocolHandler {
    
    private static final String FIELD_SEPARATOR = "\u0001"; // SOH character
    private static final Pattern FIX_MESSAGE_PATTERN = Pattern.compile("8=FIX\\.[0-9]\\.[0-9].*10=[0-9]{3}" + FIELD_SEPARATOR);
    
    /**
     * Parse a raw FIX message string into a FIXMessage object
     */
    public FIXMessage parse(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new FIXParseException("Message cannot be null or empty");
        }
        
        log.debug("Parsing FIX message: {}", sanitizeForLogging(rawMessage));
        
        try {
            FIXMessageImpl message = new FIXMessageImpl();
            
            // Split message into fields
            String[] fields = rawMessage.split(FIELD_SEPARATOR);
            
            for (String field : fields) {
                if (field.trim().isEmpty()) {
                    continue;
                }
                
                int equalPos = field.indexOf('=');
                if (equalPos == -1) {
                    throw new FIXParseException("Invalid field format: " + field);
                }
                
                String tagStr = field.substring(0, equalPos);
                String value = field.substring(equalPos + 1);
                
                try {
                    int tag = Integer.parseInt(tagStr);
                    message.setField(tag, value);
                } catch (NumberFormatException e) {
                    throw new FIXParseException("Invalid tag number: " + tagStr);
                }
            }
            
            // Validate basic structure
            if (!isBasicStructureValid(message)) {
                throw new FIXParseException("Message missing required header fields");
            }
            
            // Verify checksum if present
            if (message.getChecksum() != null && !verifyChecksum(rawMessage, message.getChecksum())) {
                throw new FIXParseException("Checksum validation failed");
            }
            
            log.debug("Successfully parsed message: {}", message);
            return message;
            
        } catch (Exception e) {
            log.error("Failed to parse FIX message: {}", e.getMessage());
            throw new FIXParseException("Failed to parse FIX message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Format a FIXMessage object into wire format string
     */
    public String format(FIXMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        try {
            String formatted = message.toFixString();
            log.debug("Formatted message: {}", sanitizeForLogging(formatted));
            return formatted;
        } catch (Exception e) {
            log.error("Failed to format FIX message: {}", e.getMessage());
            throw new FIXFormatException("Failed to format FIX message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate a FIX message according to protocol specifications
     */
    public ValidationResult validate(FIXMessage message) {
        if (message == null) {
            return ValidationResult.invalid("Message cannot be null");
        }
        
        try {
            boolean isValid = message.isValid();
            return new ValidationResult(isValid, message.getValidationErrors());
        } catch (Exception e) {
            log.error("Validation error: {}", e.getMessage());
            return ValidationResult.invalid("Validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if the raw message has basic FIX structure
     */
    public boolean isValidFixMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return false;
        }
        
        return FIX_MESSAGE_PATTERN.matcher(rawMessage).matches();
    }
    
    private boolean isBasicStructureValid(FIXMessage message) {
        return message.getBeginString() != null && 
               message.getMessageType() != null;
    }
    
    private boolean verifyChecksum(String rawMessage, String expectedChecksum) {
        try {
            // Find the checksum field and calculate checksum for everything before it
            int checksumPos = rawMessage.lastIndexOf("10=");
            if (checksumPos == -1) {
                return false;
            }
            
            String messageForChecksum = rawMessage.substring(0, checksumPos);
            int sum = 0;
            for (char c : messageForChecksum.toCharArray()) {
                sum += c;
            }
            
            String calculatedChecksum = String.format("%03d", sum % 256);
            return calculatedChecksum.equals(expectedChecksum);
            
        } catch (Exception e) {
            log.warn("Checksum verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    private String sanitizeForLogging(String message) {
        // Replace SOH with | for readable logging
        return message.replace(FIELD_SEPARATOR, "|");
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final java.util.List<String> errors;
        
        public ValidationResult(boolean valid, java.util.List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? new java.util.ArrayList<>(errors) : new java.util.ArrayList<>();
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, new java.util.ArrayList<String>());
        }
        
        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, java.util.Arrays.asList(error));
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + String.join(", ", errors);
        }
    }
}