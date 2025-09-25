package com.fixserver.protocol;

import com.fixserver.core.FIXMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive FIX message validator
 */
@Component
public class FIXValidator {
    
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^[YN]$");
    private static final Pattern INT_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern SEQNUM_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern CHAR_PATTERN = Pattern.compile("^.$");
    private static final Pattern UTCTIMESTAMP_PATTERN = Pattern.compile("^\\d{8}-\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?$");
    private static final Pattern UTCDATEONLY_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern UTCTIMEONLY_PATTERN = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?$");
    
    /**
     * Validate a FIX message according to protocol specifications
     */
    public ValidationResult validateMessage(FIXMessage message) {
        List<String> errors = new ArrayList<>();
        
        if (message == null) {
            errors.add("Message cannot be null");
            return new ValidationResult(false, errors);
        }
        
        // Validate required header fields
        validateRequiredFields(message, errors);
        
        // Validate field formats
        validateFieldFormats(message, errors);
        
        // Validate message type specific rules
        validateMessageTypeRules(message, errors);
        
        // Validate sequence numbers
        validateSequenceNumbers(message, errors);
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private void validateRequiredFields(FIXMessage message, List<String> errors) {
        // Standard header fields
        if (isEmpty(message.getBeginString())) {
            errors.add("BeginString (8) is required");
        }
        
        if (isEmpty(message.getMessageType())) {
            errors.add("MsgType (35) is required");
        }
        
        if (isEmpty(message.getSenderCompId())) {
            errors.add("SenderCompID (49) is required");
        }
        
        if (isEmpty(message.getTargetCompId())) {
            errors.add("TargetCompID (56) is required");
        }
        
        if (message.getMessageSequenceNumber() <= 0) {
            errors.add("MsgSeqNum (34) is required and must be positive");
        }
        
        if (message.getSendingTime() == null) {
            errors.add("SendingTime (52) is required");
        }
    }
    
    private void validateFieldFormats(FIXMessage message, List<String> errors) {
        // Validate each field according to its type
        for (Map.Entry<Integer, String> entry : message.getAllFields().entrySet()) {
            int tag = entry.getKey();
            String value = entry.getValue();
            
            if (value == null || value.isEmpty()) {
                continue; // Skip empty values (handled by required field validation)
            }
            
            FieldDefinition.FieldType fieldType = FieldDefinition.getFieldType(tag);
            String fieldName = FieldDefinition.getFieldName(tag);
            
            switch (fieldType) {
                case BOOLEAN:
                    if (!BOOLEAN_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be Y or N, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case INT:
                case LENGTH:
                case NUMINGROUP:
                    if (!INT_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be an integer, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case FLOAT:
                case PRICE:
                case QTY:
                    if (!FLOAT_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be a number, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case SEQNUM:
                    if (!SEQNUM_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be a positive integer, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case CHAR:
                    if (!CHAR_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be a single character, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case UTCTIMESTAMP:
                    if (!UTCTIMESTAMP_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be in format YYYYMMDD-HH:MM:SS, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case UTCDATEONLY:
                    if (!UTCDATEONLY_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be in format YYYYMMDD, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case UTCTIMEONLY:
                    if (!UTCTIMEONLY_PATTERN.matcher(value).matches()) {
                        errors.add(String.format("%s (%d) must be in format HH:MM:SS, got: %s", fieldName, tag, value));
                    }
                    break;
                    
                case STRING:
                case DATA:
                default:
                    // String fields - basic validation
                    if (value.length() > 1000) { // Reasonable limit
                        errors.add(String.format("%s (%d) exceeds maximum length", fieldName, tag));
                    }
                    break;
            }
        }
    }
    
    private void validateMessageTypeRules(FIXMessage message, List<String> errors) {
        String msgType = message.getMessageType();
        if (msgType == null) {
            return; // Already handled in required fields
        }
        
        MessageType messageType = MessageType.fromValue(msgType);
        if (messageType == null) {
            errors.add("Unknown message type: " + msgType);
            return;
        }
        
        // Message-specific validation rules
        switch (messageType) {
            case LOGON:
                validateLogonMessage(message, errors);
                break;
                
            case NEW_ORDER_SINGLE:
                validateNewOrderMessage(message, errors);
                break;
                
            case EXECUTION_REPORT:
                validateExecutionReportMessage(message, errors);
                break;
                
            case HEARTBEAT:
                // Heartbeat messages are simple, no additional validation needed
                break;
                
            case TEST_REQUEST:
                if (isEmpty(message.getField(112))) { // TestReqID
                    errors.add("TestReqID (112) is required for Test Request messages");
                }
                break;
                
            default:
                // Other message types - basic validation already done
                break;
        }
    }
    
    private void validateLogonMessage(FIXMessage message, List<String> errors) {
        // EncryptMethod is required
        if (isEmpty(message.getField(98))) {
            errors.add("EncryptMethod (98) is required for Logon messages");
        }
        
        // HeartBtInt is required
        if (isEmpty(message.getField(108))) {
            errors.add("HeartBtInt (108) is required for Logon messages");
        } else {
            try {
                int heartbeat = Integer.parseInt(message.getField(108));
                if (heartbeat < 0) {
                    errors.add("HeartBtInt (108) must be non-negative");
                }
            } catch (NumberFormatException e) {
                errors.add("HeartBtInt (108) must be a valid integer");
            }
        }
    }
    
    private void validateNewOrderMessage(FIXMessage message, List<String> errors) {
        // ClOrdID is required
        if (isEmpty(message.getField(11))) {
            errors.add("ClOrdID (11) is required for New Order Single messages");
        }
        
        // Symbol is required
        if (isEmpty(message.getField(55))) {
            errors.add("Symbol (55) is required for New Order Single messages");
        }
        
        // Side is required
        if (isEmpty(message.getField(54))) {
            errors.add("Side (54) is required for New Order Single messages");
        }
        
        // OrderQty is required
        if (isEmpty(message.getField(38))) {
            errors.add("OrderQty (38) is required for New Order Single messages");
        }
        
        // OrdType is required
        if (isEmpty(message.getField(40))) {
            errors.add("OrdType (40) is required for New Order Single messages");
        }
    }
    
    private void validateExecutionReportMessage(FIXMessage message, List<String> errors) {
        // OrderID is required
        if (isEmpty(message.getField(37))) {
            errors.add("OrderID (37) is required for Execution Report messages");
        }
        
        // ExecID is required
        if (isEmpty(message.getField(17))) {
            errors.add("ExecID (17) is required for Execution Report messages");
        }
        
        // ExecType is required
        if (isEmpty(message.getField(150))) {
            errors.add("ExecType (150) is required for Execution Report messages");
        }
        
        // OrdStatus is required
        if (isEmpty(message.getField(39))) {
            errors.add("OrdStatus (39) is required for Execution Report messages");
        }
        
        // LeavesQty is required
        if (isEmpty(message.getField(151))) {
            errors.add("LeavesQty (151) is required for Execution Report messages");
        }
        
        // CumQty is required
        if (isEmpty(message.getField(14))) {
            errors.add("CumQty (14) is required for Execution Report messages");
        }
    }
    
    private void validateSequenceNumbers(FIXMessage message, List<String> errors) {
        int seqNum = message.getMessageSequenceNumber();
        if (seqNum <= 0) {
            errors.add("Message sequence number must be positive");
        }
        
        // Additional sequence number validation could be added here
        // (e.g., checking against expected sequence numbers)
    }
    
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + String.join(", ", errors);
        }
    }
}