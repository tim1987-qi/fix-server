package com.fixserver.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

class FIXMessageImplTest {
    
    private FIXMessageImpl message;
    private static final DateTimeFormatter FIX_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
    
    @BeforeEach
    void setUp() {
        message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
    }
    
    @Test
    void testBasicFieldOperations() {
        assertEquals("FIX.4.4", message.getBeginString());
        assertEquals("D", message.getMessageType());
        assertEquals("SENDER", message.getSenderCompId());
        assertEquals("TARGET", message.getTargetCompId());
        assertEquals(1, message.getMessageSequenceNumber());
    }
    
    @Test
    void testFieldSetAndGet() {
        message.setField(100, "TEST_VALUE");
        assertEquals("TEST_VALUE", message.getField(100));
        
        message.setField(100, null);
        assertNull(message.getField(100));
    }
    
    @Test
    void testSendingTime() {
        LocalDateTime now = LocalDateTime.now();
        message.setField(FIXMessage.SENDING_TIME, now.format(FIX_TIME_FORMAT));
        
        LocalDateTime parsed = message.getSendingTime();
        assertNotNull(parsed);
        assertEquals(now.getYear(), parsed.getYear());
        assertEquals(now.getMonth(), parsed.getMonth());
        assertEquals(now.getDayOfMonth(), parsed.getDayOfMonth());
    }
    
    @Test
    void testValidMessage() {
        assertTrue(message.isValid());
        assertTrue(message.getValidationErrors().isEmpty());
    }
    
    @Test
    void testInvalidMessage_MissingRequiredFields() {
        FIXMessageImpl invalidMessage = new FIXMessageImpl();
        
        assertFalse(invalidMessage.isValid());
        assertFalse(invalidMessage.getValidationErrors().isEmpty());
        
        assertTrue(invalidMessage.getValidationErrors().stream()
            .anyMatch(error -> error.contains("BeginString")));
        assertTrue(invalidMessage.getValidationErrors().stream()
            .anyMatch(error -> error.contains("MessageType")));
    }
    
    @Test
    void testInvalidSequenceNumber() {
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "0");
        assertFalse(message.isValid());
        
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "invalid");
        assertFalse(message.isValid());
    }
    
    @Test
    void testUnsupportedFixVersion() {
        message.setField(FIXMessage.BEGIN_STRING, "FIX.3.0");
        assertFalse(message.isValid());
        assertTrue(message.getValidationErrors().stream()
            .anyMatch(error -> error.contains("Unsupported FIX version")));
    }
    
    @Test
    void testUnknownMessageType() {
        message.setField(FIXMessage.MESSAGE_TYPE, "UNKNOWN");
        assertFalse(message.isValid());
        assertTrue(message.getValidationErrors().stream()
            .anyMatch(error -> error.contains("Unknown message type")));
    }
    
    @Test
    void testToFixString() {
        String fixString = message.toFixString();
        
        assertNotNull(fixString);
        assertTrue(fixString.startsWith("8=FIX.4.4\u0001"), "Should start with BeginString");
        assertTrue(fixString.contains("35=D\u0001"), "Should contain MessageType");
        assertTrue(fixString.contains("49=SENDER\u0001"), "Should contain SenderCompID");
        assertTrue(fixString.contains("56=TARGET\u0001"), "Should contain TargetCompID");
        assertTrue(fixString.contains("34=1\u0001"), "Should contain MsgSeqNum");
        assertTrue(fixString.contains("10="), "Should contain checksum field");
        assertTrue(fixString.endsWith("\u0001"), "Should end with SOH");
    }
    
    @Test
    void testChecksumCalculation() {
        String fixString = message.toFixString();
        String checksum = message.getChecksum();
        
        assertNotNull(checksum, "Checksum should not be null after toFixString()");
        assertEquals(3, checksum.length(), "Checksum should be 3 digits");
        assertTrue(fixString.contains("10=" + checksum), "FIX string should contain the checksum");
    }
    
    @Test
    void testEqualsAndHashCode() {
        FIXMessageImpl message2 = new FIXMessageImpl("FIX.4.4", "D");
        message2.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message2.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message2.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        message2.setField(FIXMessage.SENDING_TIME, message.getField(FIXMessage.SENDING_TIME));
        
        assertEquals(message, message2);
        assertEquals(message.hashCode(), message2.hashCode());
    }
    
    @Test
    void testToString() {
        String toString = message.toString();
        assertTrue(toString.contains("type=D"));
        assertTrue(toString.contains("sender=SENDER"));
        assertTrue(toString.contains("target=TARGET"));
        assertTrue(toString.contains("seqNum=1"));
    }
    
    @Test
    void testGetAllFields() {
        Map<Integer, String> allFields = message.getAllFields();
        
        assertNotNull(allFields);
        assertTrue(allFields.containsKey(FIXMessage.BEGIN_STRING));
        assertTrue(allFields.containsKey(FIXMessage.MESSAGE_TYPE));
        assertTrue(allFields.containsKey(FIXMessage.SENDER_COMP_ID));
        
        // Ensure it's a copy, not the original
        allFields.clear();
        assertFalse(message.getAllFields().isEmpty());
    }
}