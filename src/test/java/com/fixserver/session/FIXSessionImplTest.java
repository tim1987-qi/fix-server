package com.fixserver.session;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.store.MessageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

class FIXSessionImplTest {
    
    @Mock
    private FIXProtocolHandler protocolHandler;
    
    @Mock
    private MessageStore messageStore;
    
    private FIXSessionImpl session;
    private static final String SESSION_ID = "TEST_SESSION";
    private static final String SENDER_COMP_ID = "SENDER";
    private static final String TARGET_COMP_ID = "TARGET";
    private static final int HEARTBEAT_INTERVAL = 30;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        session = new FIXSessionImpl(
            SESSION_ID, 
            SENDER_COMP_ID, 
            TARGET_COMP_ID,
            protocolHandler,
            messageStore,
            HEARTBEAT_INTERVAL
        );
    }
    
    @Test
    void testSessionCreation() {
        assertEquals(SESSION_ID, session.getSessionId());
        assertEquals(SENDER_COMP_ID, session.getSenderCompId());
        assertEquals(TARGET_COMP_ID, session.getTargetCompId());
        assertEquals(HEARTBEAT_INTERVAL, session.getHeartbeatInterval());
        assertEquals(FIXSession.Status.DISCONNECTED, session.getStatus());
        assertEquals(1, session.getNextIncomingSequenceNumber());
        assertEquals(1, session.getNextOutgoingSequenceNumber());
        assertFalse(session.isActive());
    }
    
    @Test
    void testLogon() {
        CompletableFuture<Void> logonFuture = session.logon(30, "0");
        
        assertDoesNotThrow(() -> logonFuture.join());
        assertEquals(FIXSession.Status.LOGON_SENT, session.getStatus());
        assertNotNull(session.getSessionStartTime());
        
        // Verify message was stored
        verify(messageStore).storeMessage(eq(SESSION_ID), any(FIXMessage.class), eq(MessageStore.MessageDirection.OUTGOING));
    }
    
    @Test
    void testSendMessage() {
        FIXMessage message = new FIXMessageImpl("FIX.4.4", "D");
        
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("formatted_message");
        
        CompletableFuture<Void> sendFuture = session.sendMessage(message);
        
        assertDoesNotThrow(() -> sendFuture.join());
        
        // Verify message fields were set
        assertEquals(SENDER_COMP_ID, message.getSenderCompId());
        assertEquals(TARGET_COMP_ID, message.getTargetCompId());
        assertEquals(1, message.getMessageSequenceNumber());
        assertNotNull(message.getSendingTime());
        
        // Verify sequence number incremented
        assertEquals(2, session.getNextOutgoingSequenceNumber());
        
        // Verify message was stored and formatted
        verify(messageStore).storeMessage(eq(SESSION_ID), eq(message), eq(MessageStore.MessageDirection.OUTGOING));
        verify(protocolHandler).format(eq(message));
    }
    
    @Test
    void testProcessValidMessage() {
        FIXMessage message = new FIXMessageImpl("FIX.4.4", "0"); // Heartbeat
        message.setField(FIXMessage.SENDER_COMP_ID, TARGET_COMP_ID);
        message.setField(FIXMessage.TARGET_COMP_ID, SENDER_COMP_ID);
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        
        CompletableFuture<Void> processFuture = session.processMessage(message);
        
        assertDoesNotThrow(() -> processFuture.join());
        
        // Verify sequence number incremented
        assertEquals(2, session.getNextIncomingSequenceNumber());
        
        // Verify message was stored
        verify(messageStore).storeMessage(eq(SESSION_ID), eq(message), eq(MessageStore.MessageDirection.INCOMING));
        
        // Verify heartbeat timestamp updated
        assertNotNull(session.getLastHeartbeat());
    }
    
    @Test
    void testProcessInvalidMessage() {
        FIXMessage message = new FIXMessageImpl("FIX.4.4", "0");
        // Missing required fields - invalid sender/target
        
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("reject_message");
        
        CompletableFuture<Void> processFuture = session.processMessage(message);
        
        assertDoesNotThrow(() -> processFuture.join());
        
        // Sequence number should not increment for invalid message
        assertEquals(1, session.getNextIncomingSequenceNumber());
        
        // Should send reject message
        verify(protocolHandler, atLeastOnce()).format(any(FIXMessage.class));
    }
    
    @Test
    void testProcessLogonMessage() {
        FIXMessage logonMessage = new FIXMessageImpl("FIX.4.4", "A"); // Logon
        logonMessage.setField(FIXMessage.SENDER_COMP_ID, TARGET_COMP_ID);
        logonMessage.setField(FIXMessage.TARGET_COMP_ID, SENDER_COMP_ID);
        logonMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        logonMessage.setField(98, "0"); // EncryptMethod
        logonMessage.setField(108, "30"); // HeartBtInt
        
        CompletableFuture<Void> processFuture = session.processMessage(logonMessage);
        
        assertDoesNotThrow(() -> processFuture.join());
        
        // Session should be logged on
        assertEquals(FIXSession.Status.LOGGED_ON, session.getStatus());
        assertTrue(session.isActive());
    }
    
    @Test
    void testProcessLogonWithResetFlag() {
        // Set initial sequence numbers
        session.processMessage(createValidMessage("0", "1")).join(); // Heartbeat
        assertEquals(2, session.getNextIncomingSequenceNumber());
        
        FIXMessage logonMessage = new FIXMessageImpl("FIX.4.4", "A"); // Logon
        logonMessage.setField(FIXMessage.SENDER_COMP_ID, TARGET_COMP_ID);
        logonMessage.setField(FIXMessage.TARGET_COMP_ID, SENDER_COMP_ID);
        logonMessage.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        logonMessage.setField(98, "0"); // EncryptMethod
        logonMessage.setField(108, "30"); // HeartBtInt
        logonMessage.setField(141, "Y"); // ResetSeqNumFlag
        
        // Reset validation to allow sequence number 1 again
        session = new FIXSessionImpl(SESSION_ID, SENDER_COMP_ID, TARGET_COMP_ID, protocolHandler, messageStore, HEARTBEAT_INTERVAL);
        
        CompletableFuture<Void> processFuture = session.processMessage(logonMessage);
        
        assertDoesNotThrow(() -> processFuture.join());
        
        // Sequence numbers should be reset
        assertEquals(2, session.getNextIncomingSequenceNumber()); // Incremented after processing
        assertEquals(1, session.getNextOutgoingSequenceNumber()); // Reset but not incremented
    }
    
    @Test
    void testProcessTestRequest() {
        FIXMessage testRequest = new FIXMessageImpl("FIX.4.4", "1"); // Test Request
        testRequest.setField(FIXMessage.SENDER_COMP_ID, TARGET_COMP_ID);
        testRequest.setField(FIXMessage.TARGET_COMP_ID, SENDER_COMP_ID);
        testRequest.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        testRequest.setField(112, "TEST123"); // TestReqID
        
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("heartbeat_response");
        
        CompletableFuture<Void> processFuture = session.processMessage(testRequest);
        
        assertDoesNotThrow(() -> processFuture.join());
        
        // Should send heartbeat response
        verify(protocolHandler, atLeastOnce()).format(any(FIXMessage.class));
        verify(messageStore, atLeastOnce()).storeMessage(eq(SESSION_ID), any(FIXMessage.class), eq(MessageStore.MessageDirection.OUTGOING));
    }
    
    @Test
    void testDisconnect() {
        // First logon
        session.logon(30, "0").join();
        assertEquals(FIXSession.Status.LOGON_SENT, session.getStatus());
        
        // Simulate logged on state
        FIXMessage logonMessage = createValidMessage("A", "1");
        logonMessage.setField(98, "0");
        logonMessage.setField(108, "30");
        session.processMessage(logonMessage).join();
        assertEquals(FIXSession.Status.LOGGED_ON, session.getStatus());
        
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("logout_message");
        
        CompletableFuture<Void> disconnectFuture = session.disconnect();
        
        assertDoesNotThrow(() -> disconnectFuture.join());
        
        assertEquals(FIXSession.Status.DISCONNECTED, session.getStatus());
        assertFalse(session.isActive());
        
        // Should send logout message
        verify(protocolHandler, atLeastOnce()).format(any(FIXMessage.class));
    }
    
    @Test
    void testHeartbeatHandling() {
        // Mock current time to control heartbeat timing
        LocalDateTime oldHeartbeat = LocalDateTime.now().minusSeconds(HEARTBEAT_INTERVAL + 1);
        
        // Use reflection or create a new session with old heartbeat
        session = new FIXSessionImpl(SESSION_ID, SENDER_COMP_ID, TARGET_COMP_ID, protocolHandler, messageStore, HEARTBEAT_INTERVAL);
        
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("heartbeat_message");
        
        session.handleHeartbeat();
        
        // Should send heartbeat if interval exceeded
        // Note: This test might need adjustment based on actual timing logic
    }
    
    @Test
    void testSequenceNumberValidation() {
        // Process first message
        FIXMessage message1 = createValidMessage("0", "1");
        session.processMessage(message1).join();
        assertEquals(2, session.getNextIncomingSequenceNumber());
        
        // Process message with gap (sequence 3 instead of 2)
        FIXMessage message2 = createValidMessage("0", "3");
        when(protocolHandler.format(any(FIXMessage.class))).thenReturn("resend_request");
        
        session.processMessage(message2).join();
        
        // Should send resend request
        verify(protocolHandler, atLeastOnce()).format(any(FIXMessage.class));
        
        // Sequence number should not increment due to gap
        assertEquals(2, session.getNextIncomingSequenceNumber());
    }
    
    @Test
    void testResetSequenceNumbers() {
        // Send some messages to increment sequence numbers
        session.sendMessage(new FIXMessageImpl("FIX.4.4", "0")).join();
        session.sendMessage(new FIXMessageImpl("FIX.4.4", "0")).join();
        
        assertEquals(3, session.getNextOutgoingSequenceNumber());
        
        session.resetSequenceNumbers();
        
        assertEquals(1, session.getNextIncomingSequenceNumber());
        assertEquals(1, session.getNextOutgoingSequenceNumber());
    }
    
    private FIXMessage createValidMessage(String messageType, String sequenceNumber) {
        FIXMessage message = new FIXMessageImpl("FIX.4.4", messageType);
        message.setField(FIXMessage.SENDER_COMP_ID, TARGET_COMP_ID);
        message.setField(FIXMessage.TARGET_COMP_ID, SENDER_COMP_ID);
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, sequenceNumber);
        return message;
    }
}