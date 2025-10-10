package com.fixserver.protocol;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class FIXValidatorTest {
    
    private FIXValidator validator;
    private FIXMessageImpl message;
    
    @BeforeEach
    void setUp() {
        validator = new FIXValidator();
        message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        // SendingTime is automatically set by FIXMessageImpl constructor
        
        // Add required fields for New Order Single (message type D)
        message.setField(11, "ORDER123"); // ClOrdID
        message.setField(55, "AAPL"); // Symbol
        message.setField(54, "1"); // Side
        message.setField(38, "100"); // OrderQty
        message.setField(40, "2"); // OrdType
    }
    
    @Test
    void testValidMessage() {
        // Message should be valid with all required fields set
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertTrue(result.isValid(), "Message should be valid: " + result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testNullMessage() {
        FIXValidator.ValidationResult result = validator.validateMessage(null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("cannot be null"));
    }
    
    @Test
    void testMissingRequiredFields() {
        FIXMessageImpl invalidMessage = new FIXMessageImpl();
        
        FIXValidator.ValidationResult result = validator.validateMessage(invalidMessage);
        assertFalse(result.isValid());
        
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("BeginString")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("MsgType")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("SenderCompID")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("TargetCompID")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("MsgSeqNum")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("SendingTime")));
    }
    
    @Test
    void testInvalidFieldFormats() {
        // Invalid boolean
        message.setField(123, "INVALID"); // GapFillFlag should be Y or N
        
        // Invalid integer
        message.setField(108, "NOT_A_NUMBER"); // HeartBtInt should be integer
        
        // Invalid sequence number
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "0"); // Should be positive
        
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
    
    @Test
    void testLogonMessageValidation() {
        message.setField(FIXMessage.MESSAGE_TYPE, "A"); // Logon
        
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertFalse(result.isValid());
        
        // Should require EncryptMethod and HeartBtInt
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("EncryptMethod")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("HeartBtInt")));
        
        // Add required fields
        message.setField(98, "0"); // EncryptMethod
        message.setField(108, "30"); // HeartBtInt
        
        result = validator.validateMessage(message);
        assertTrue(result.isValid());
    }
    
    @Test
    void testNewOrderSingleValidation() {
        // Create a new message without the order fields
        FIXMessageImpl orderMessage = new FIXMessageImpl("FIX.4.4", "D");
        orderMessage.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        orderMessage.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        orderMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        
        FIXValidator.ValidationResult result = validator.validateMessage(orderMessage);
        assertFalse(result.isValid());
        
        // Should require order-specific fields
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("ClOrdID")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Symbol")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Side")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("OrderQty")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("OrdType")));
        
        // Add required fields
        orderMessage.setField(11, "ORDER123"); // ClOrdID
        orderMessage.setField(55, "AAPL"); // Symbol
        orderMessage.setField(54, "1"); // Side
        orderMessage.setField(38, "100"); // OrderQty
        orderMessage.setField(40, "2"); // OrdType
        
        result = validator.validateMessage(orderMessage);
        assertTrue(result.isValid());
    }
    
    @Test
    void testExecutionReportValidation() {
        message.setField(FIXMessage.MESSAGE_TYPE, "8"); // Execution Report
        
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertFalse(result.isValid());
        
        // Should require execution report specific fields
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("OrderID")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("ExecID")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("ExecType")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("OrdStatus")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("LeavesQty")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("CumQty")));
    }
    
    @Test
    void testTestRequestValidation() {
        message.setField(FIXMessage.MESSAGE_TYPE, "1"); // Test Request
        
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertFalse(result.isValid());
        
        // Should require TestReqID
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("TestReqID")));
        
        message.setField(112, "TEST123"); // TestReqID
        result = validator.validateMessage(message);
        assertTrue(result.isValid());
    }
    
    @Test
    void testHeartbeatValidation() {
        message.setField(FIXMessage.MESSAGE_TYPE, "0"); // Heartbeat
        
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertTrue(result.isValid()); // Heartbeat has no additional requirements
    }
    
    @Test
    void testFieldTypeValidation() {
        // Test boolean field
        message.setField(123, "Y"); // Valid boolean
        FIXValidator.ValidationResult result = validator.validateMessage(message);
        assertTrue(result.isValid(), "Message with valid boolean should be valid: " + result.getErrors());
        
        message.setField(123, "INVALID"); // Invalid boolean
        result = validator.validateMessage(message);
        assertFalse(result.isValid(), "Message with invalid boolean should be invalid");
        
        // Test integer field
        message.setField(108, "30"); // Valid integer
        message.setField(123, "Y"); // Fix the boolean field
        result = validator.validateMessage(message);
        assertTrue(result.isValid(), "Message with valid integer should be valid: " + result.getErrors());
        
        message.setField(108, "NOT_NUMBER"); // Invalid integer
        result = validator.validateMessage(message);
        assertFalse(result.isValid(), "Message with invalid integer should be invalid");
    }
    
    @Test
    void testValidationResultToString() {
        FIXValidator.ValidationResult validResult = new FIXValidator.ValidationResult(true, null);
        assertEquals("Valid", validResult.toString());
        
        FIXValidator.ValidationResult invalidResult = 
            new FIXValidator.ValidationResult(false, java.util.Arrays.asList("Error 1", "Error 2"));
        assertTrue(invalidResult.toString().contains("Invalid"));
        assertTrue(invalidResult.toString().contains("Error 1"));
        assertTrue(invalidResult.toString().contains("Error 2"));
    }
}