package com.fixserver.protocol;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class FIXProtocolHandlerTest {
    
    private FIXProtocolHandler handler;
    private static final String SOH = "\u0001";
    
    @BeforeEach
    void setUp() {
        handler = new FIXProtocolHandler();
    }
    
    @Test
    void testParseValidMessage() {
        String rawMessage = "8=FIX.4.4" + SOH + "35=D" + SOH + "49=SENDER" + SOH + 
                           "56=TARGET" + SOH + "34=1" + SOH + "52=20231201-10:30:00" + SOH + "10=123" + SOH;
        
        FIXMessage message = handler.parse(rawMessage);
        
        assertNotNull(message);
        assertEquals("FIX.4.4", message.getBeginString());
        assertEquals("D", message.getMessageType());
        assertEquals("SENDER", message.getSenderCompId());
        assertEquals("TARGET", message.getTargetCompId());
        assertEquals(1, message.getMessageSequenceNumber());
    }
    
    @Test
    void testParseNullMessage() {
        assertThrows(FIXParseException.class, () -> handler.parse(null));
    }
    
    @Test
    void testParseEmptyMessage() {
        assertThrows(FIXParseException.class, () -> handler.parse(""));
        assertThrows(FIXParseException.class, () -> handler.parse("   "));
    }
    
    @Test
    void testParseInvalidFieldFormat() {
        String rawMessage = "8=FIX.4.4" + SOH + "35D" + SOH; // Missing equals sign
        
        assertThrows(FIXParseException.class, () -> handler.parse(rawMessage));
    }
    
    @Test
    void testParseInvalidTagNumber() {
        String rawMessage = "8=FIX.4.4" + SOH + "ABC=D" + SOH; // Non-numeric tag
        
        assertThrows(FIXParseException.class, () -> handler.parse(rawMessage));
    }
    
    @Test
    void testFormatValidMessage() {
        FIXMessageImpl message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        
        String formatted = handler.format(message);
        
        assertNotNull(formatted);
        assertTrue(formatted.startsWith("8=FIX.4.4" + SOH));
        assertTrue(formatted.contains("35=D" + SOH));
        assertTrue(formatted.contains("49=SENDER" + SOH));
        assertTrue(formatted.contains("56=TARGET" + SOH));
        assertTrue(formatted.contains("34=1" + SOH));
        assertTrue(formatted.endsWith(SOH));
    }
    
    @Test
    void testFormatNullMessage() {
        assertThrows(IllegalArgumentException.class, () -> handler.format(null));
    }
    
    @Test
    void testValidateValidMessage() {
        FIXMessageImpl message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        
        FIXProtocolHandler.ValidationResult result = handler.validate(message);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateInvalidMessage() {
        FIXMessageImpl message = new FIXMessageImpl(); // Missing required fields
        
        FIXProtocolHandler.ValidationResult result = handler.validate(message);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateNullMessage() {
        FIXProtocolHandler.ValidationResult result = handler.validate(null);
        
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("cannot be null"));
    }
    
    @Test
    void testIsValidFixMessage() {
        String validMessage = "8=FIX.4.4" + SOH + "35=D" + SOH + "10=123" + SOH;
        assertTrue(handler.isValidFixMessage(validMessage));
        
        String invalidMessage = "Invalid message";
        assertFalse(handler.isValidFixMessage(invalidMessage));
        
        assertFalse(handler.isValidFixMessage(null));
        assertFalse(handler.isValidFixMessage(""));
    }
    
    @Test
    void testParseAndFormatRoundTrip() {
        String originalMessage = "8=FIX.4.4" + SOH + "35=D" + SOH + "49=SENDER" + SOH + 
                                "56=TARGET" + SOH + "34=1" + SOH + "52=20231201-10:30:00" + SOH;
        
        FIXMessage parsed = handler.parse(originalMessage);
        String formatted = handler.format(parsed);
        
        assertNotNull(formatted);
        assertTrue(formatted.contains("8=FIX.4.4"));
        assertTrue(formatted.contains("35=D"));
        assertTrue(formatted.contains("49=SENDER"));
        assertTrue(formatted.contains("56=TARGET"));
        assertTrue(formatted.contains("34=1"));
    }
    
    @Test
    void testValidationResultToString() {
        FIXProtocolHandler.ValidationResult validResult = FIXProtocolHandler.ValidationResult.valid();
        assertEquals("Valid", validResult.toString());
        
        FIXProtocolHandler.ValidationResult invalidResult = 
            FIXProtocolHandler.ValidationResult.invalid("Test error");
        assertTrue(invalidResult.toString().contains("Invalid"));
        assertTrue(invalidResult.toString().contains("Test error"));
    }
    
    @Test
    void testValidationResultStaticMethods() {
        FIXProtocolHandler.ValidationResult validResult = FIXProtocolHandler.ValidationResult.valid();
        assertTrue(validResult.isValid());
        assertTrue(validResult.getErrors().isEmpty());
        
        FIXProtocolHandler.ValidationResult invalidResult = 
            FIXProtocolHandler.ValidationResult.invalid("Error message");
        assertFalse(invalidResult.isValid());
        assertEquals(1, invalidResult.getErrors().size());
        assertEquals("Error message", invalidResult.getErrors().get(0));
    }
}