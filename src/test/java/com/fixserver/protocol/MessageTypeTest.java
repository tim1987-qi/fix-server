package com.fixserver.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {
    
    @Test
    void testFromValue() {
        assertEquals(MessageType.HEARTBEAT, MessageType.fromValue("0"));
        assertEquals(MessageType.LOGON, MessageType.fromValue("A"));
        assertEquals(MessageType.NEW_ORDER_SINGLE, MessageType.fromValue("D"));
        assertNull(MessageType.fromValue("INVALID"));
        assertNull(MessageType.fromValue(null));
    }
    
    @Test
    void testIsValid() {
        assertTrue(MessageType.isValid("0"));
        assertTrue(MessageType.isValid("A"));
        assertTrue(MessageType.isValid("D"));
        assertFalse(MessageType.isValid("INVALID"));
        assertFalse(MessageType.isValid(null));
    }
    
    @Test
    void testSessionLevelMessages() {
        assertTrue(MessageType.HEARTBEAT.isSessionLevel());
        assertTrue(MessageType.LOGON.isSessionLevel());
        assertTrue(MessageType.LOGOUT.isSessionLevel());
        assertTrue(MessageType.TEST_REQUEST.isSessionLevel());
        assertTrue(MessageType.RESEND_REQUEST.isSessionLevel());
        assertTrue(MessageType.REJECT.isSessionLevel());
        assertTrue(MessageType.SEQUENCE_RESET.isSessionLevel());
        
        assertFalse(MessageType.HEARTBEAT.isApplicationLevel());
    }
    
    @Test
    void testApplicationLevelMessages() {
        assertTrue(MessageType.NEW_ORDER_SINGLE.isApplicationLevel());
        assertTrue(MessageType.EXECUTION_REPORT.isApplicationLevel());
        assertTrue(MessageType.ORDER_CANCEL_REQUEST.isApplicationLevel());
        
        assertFalse(MessageType.NEW_ORDER_SINGLE.isSessionLevel());
    }
    
    @Test
    void testGetters() {
        MessageType heartbeat = MessageType.HEARTBEAT;
        assertEquals("0", heartbeat.getValue());
        assertEquals("Heartbeat", heartbeat.getDescription());
    }
    
    @Test
    void testToString() {
        String toString = MessageType.HEARTBEAT.toString();
        assertTrue(toString.contains("0"));
        assertTrue(toString.contains("Heartbeat"));
    }
}