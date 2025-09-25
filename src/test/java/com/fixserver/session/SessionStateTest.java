package com.fixserver.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDateTime;

class SessionStateTest {
    
    private SessionState sessionState;
    private static final String SESSION_ID = "TEST_SESSION";
    private static final String SENDER_COMP_ID = "SENDER";
    private static final String TARGET_COMP_ID = "TARGET";
    
    @BeforeEach
    void setUp() {
        sessionState = new SessionState(SESSION_ID, SENDER_COMP_ID, TARGET_COMP_ID);
    }
    
    @Test
    void testInitialState() {
        assertEquals(SESSION_ID, sessionState.getSessionId());
        assertEquals(SENDER_COMP_ID, sessionState.getSenderCompId());
        assertEquals(TARGET_COMP_ID, sessionState.getTargetCompId());
        assertEquals(FIXSession.Status.DISCONNECTED, sessionState.getStatus());
        assertEquals(1, sessionState.getIncomingSequenceNumber());
        assertEquals(1, sessionState.getOutgoingSequenceNumber());
        assertEquals(Duration.ofSeconds(30), sessionState.getHeartbeatInterval());
        assertEquals(0, sessionState.getTotalMessagesReceived());
        assertEquals(0, sessionState.getTotalMessagesSent());
        assertNotNull(sessionState.getLastHeartbeat());
    }
    
    @Test
    void testStatusChange() {
        sessionState.setStatus(FIXSession.Status.CONNECTING);
        assertEquals(FIXSession.Status.CONNECTING, sessionState.getStatus());
        
        sessionState.setStatus(FIXSession.Status.LOGGED_ON);
        assertEquals(FIXSession.Status.LOGGED_ON, sessionState.getStatus());
    }
    
    @Test
    void testSequenceNumberOperations() {
        // Test increment operations
        sessionState.incrementIncomingSequenceNumber();
        assertEquals(2, sessionState.getIncomingSequenceNumber());
        assertEquals(1, sessionState.getTotalMessagesReceived());
        
        sessionState.incrementOutgoingSequenceNumber();
        assertEquals(2, sessionState.getOutgoingSequenceNumber());
        assertEquals(1, sessionState.getTotalMessagesSent());
        
        // Test multiple increments
        sessionState.incrementIncomingSequenceNumber();
        sessionState.incrementOutgoingSequenceNumber();
        assertEquals(3, sessionState.getIncomingSequenceNumber());
        assertEquals(3, sessionState.getOutgoingSequenceNumber());
        assertEquals(2, sessionState.getTotalMessagesReceived());
        assertEquals(2, sessionState.getTotalMessagesSent());
    }
    
    @Test
    void testResetSequenceNumbers() {
        // Increment sequence numbers
        sessionState.incrementIncomingSequenceNumber();
        sessionState.incrementOutgoingSequenceNumber();
        assertEquals(2, sessionState.getIncomingSequenceNumber());
        assertEquals(2, sessionState.getOutgoingSequenceNumber());
        
        // Reset
        sessionState.resetSequenceNumbers();
        assertEquals(1, sessionState.getIncomingSequenceNumber());
        assertEquals(1, sessionState.getOutgoingSequenceNumber());
        
        // Message counts should remain
        assertEquals(1, sessionState.getTotalMessagesReceived());
        assertEquals(1, sessionState.getTotalMessagesSent());
    }
    
    @Test
    void testHeartbeatTiming() {
        LocalDateTime now = LocalDateTime.now();
        
        // Set recent heartbeat - should not be due
        sessionState.setLastHeartbeat(now.minusSeconds(10));
        assertFalse(sessionState.isHeartbeatDue());
        assertFalse(sessionState.isTimedOut());
        
        // Set old heartbeat - should be due
        sessionState.setLastHeartbeat(now.minusSeconds(35));
        assertTrue(sessionState.isHeartbeatDue());
        assertFalse(sessionState.isTimedOut());
        
        // Set very old heartbeat - should be timed out
        sessionState.setLastHeartbeat(now.minusSeconds(70));
        assertTrue(sessionState.isHeartbeatDue());
        assertTrue(sessionState.isTimedOut());
    }
    
    @Test
    void testSessionDuration() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
        sessionState.setSessionStartTime(startTime);
        
        Duration duration = sessionState.getSessionDuration();
        assertTrue(duration.toMinutes() >= 4); // Should be around 5 minutes, allowing for test execution time
        assertTrue(duration.toMinutes() <= 6);
    }
    
    @Test
    void testMessageRate() {
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(60); // 1 minute ago
        sessionState.setSessionStartTime(startTime);
        
        // Send some messages
        for (int i = 0; i < 10; i++) {
            sessionState.incrementIncomingSequenceNumber();
            sessionState.incrementOutgoingSequenceNumber();
        }
        
        double rate = sessionState.getMessageRate();
        assertTrue(rate > 0);
        // Should be around 20 messages per 60 seconds = ~0.33 messages/second
        assertTrue(rate >= 0.2 && rate <= 0.5);
    }
    
    @Test
    void testMessageRateWithNoSession() {
        // No session start time set
        double rate = sessionState.getMessageRate();
        assertEquals(0.0, rate);
    }
    
    @Test
    void testTimeSinceLastHeartbeat() {
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(30);
        sessionState.setLastHeartbeat(pastTime);
        
        Duration timeSince = sessionState.getTimeSinceLastHeartbeat();
        assertTrue(timeSince.getSeconds() >= 29); // Allow for test execution time
        assertTrue(timeSince.getSeconds() <= 31);
    }
    
    @Test
    void testTimeSinceLastHeartbeatWithNullHeartbeat() {
        sessionState.setLastHeartbeat(null);
        
        Duration timeSince = sessionState.getTimeSinceLastHeartbeat();
        assertEquals(Duration.ZERO, timeSince);
    }
    
    @Test
    void testHeartbeatIntervalConfiguration() {
        Duration customInterval = Duration.ofSeconds(60);
        sessionState.setHeartbeatInterval(customInterval);
        
        assertEquals(customInterval, sessionState.getHeartbeatInterval());
        
        // Test heartbeat due calculation with custom interval
        LocalDateTime now = LocalDateTime.now();
        sessionState.setLastHeartbeat(now.minusSeconds(30));
        assertFalse(sessionState.isHeartbeatDue()); // 30 seconds < 60 seconds interval
        
        sessionState.setLastHeartbeat(now.minusSeconds(70));
        assertTrue(sessionState.isHeartbeatDue()); // 70 seconds > 60 seconds interval
    }
    
    @Test
    void testErrorHandling() {
        String errorMessage = "Test error message";
        sessionState.setLastError(errorMessage);
        
        assertEquals(errorMessage, sessionState.getLastError());
    }
    
    @Test
    void testToString() {
        sessionState.setStatus(FIXSession.Status.LOGGED_ON);
        sessionState.incrementIncomingSequenceNumber();
        sessionState.incrementOutgoingSequenceNumber();
        
        String toString = sessionState.toString();
        
        assertTrue(toString.contains(SESSION_ID));
        assertTrue(toString.contains("LOGGED_ON"));
        assertTrue(toString.contains("in=2"));
        assertTrue(toString.contains("out=2"));
    }
}