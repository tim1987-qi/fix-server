package com.fixserver.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class SessionTimeoutHandlerTest {
    
    @Mock
    private SessionTimeoutHandler.TimeoutCallback callback;
    
    private SessionTimeoutHandler timeoutHandler;
    private static final String SESSION_ID = "TEST_SESSION";
    private static final int TIMEOUT_SECONDS = 2; // 2 seconds for faster testing
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        timeoutHandler = new SessionTimeoutHandler();
        timeoutHandler.start();
    }
    
    @AfterEach
    void tearDown() {
        timeoutHandler.stop();
    }
    
    @Test
    void testStartStop() {
        SessionTimeoutHandler handler = new SessionTimeoutHandler();
        
        handler.start();
        // Starting again should not cause issues
        handler.start();
        
        handler.stop();
        // Stopping again should not cause issues
        handler.stop();
    }
    
    @Test
    void testRegisterUnregisterSession() {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        // Should be able to update activity for registered session
        timeoutHandler.updateActivity(SESSION_ID);
        
        // Should not be timed out initially
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
        
        timeoutHandler.unregisterSession(SESSION_ID);
        
        // Should not affect unregistered session
        timeoutHandler.updateActivity(SESSION_ID);
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
    }
    
    @Test
    void testRegisterWithDefaultTimeout() {
        timeoutHandler.registerSession(SESSION_ID, callback);
        
        SessionTimeoutHandler.TimeoutStats stats = timeoutHandler.getTimeoutStats(SESSION_ID);
        assertNotNull(stats);
        assertEquals(120, stats.getTimeoutSeconds()); // Default timeout
    }
    
    @Test
    void testUpdateActivity() {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        // Initially should not be timed out
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
        
        timeoutHandler.updateActivity(SESSION_ID);
        
        // Still should not be timed out after activity update
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
    }
    
    @Test
    void testTimeout() throws InterruptedException {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        // Initially should not be timed out
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
        
        // Wait for timeout to occur
        Thread.sleep((TIMEOUT_SECONDS + 1) * 1000);
        
        // Now should be timed out
        assertTrue(timeoutHandler.isTimedOut(SESSION_ID));
    }
    
    @Test
    void testTimeUntilTimeout() {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        long timeUntil = timeoutHandler.getTimeUntilTimeout(SESSION_ID);
        assertTrue(timeUntil > 0);
        assertTrue(timeUntil <= TIMEOUT_SECONDS);
    }
    
    @Test
    void testTimeUntilTimeoutForUnknownSession() {
        long timeUntil = timeoutHandler.getTimeUntilTimeout("UNKNOWN");
        assertEquals(-1, timeUntil);
    }
    
    @Test
    void testManualTimeout() throws InterruptedException {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        
        doAnswer(invocation -> {
            timeoutLatch.countDown();
            return null;
        }).when(callback).onSessionTimeout(eq(SESSION_ID), any());
        
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        // Manually trigger timeout
        timeoutHandler.triggerTimeout(SESSION_ID);
        
        // Wait for timeout callback
        assertTrue(timeoutLatch.await(2, TimeUnit.SECONDS));
        
        verify(callback).onSessionTimeout(eq(SESSION_ID), eq(SessionTimeoutHandler.TimeoutReason.MANUAL_TRIGGER));
        assertTrue(timeoutHandler.isTimedOut(SESSION_ID));
    }
    
    @Test
    void testTimeoutCallback() throws InterruptedException {
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        
        doAnswer(invocation -> {
            timeoutLatch.countDown();
            return null;
        }).when(callback).onSessionTimeout(eq(SESSION_ID), any());
        
        timeoutHandler.registerSession(SESSION_ID, 1, callback); // 1 second timeout
        
        // Wait for timeout to be triggered automatically
        assertTrue(timeoutLatch.await(5, TimeUnit.SECONDS));
        
        verify(callback).onSessionTimeout(eq(SESSION_ID), eq(SessionTimeoutHandler.TimeoutReason.INACTIVITY));
    }
    
    @Test
    void testTimeoutStats() {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        SessionTimeoutHandler.TimeoutStats stats = timeoutHandler.getTimeoutStats(SESSION_ID);
        
        assertNotNull(stats);
        assertEquals(SESSION_ID, stats.getSessionId());
        assertNotNull(stats.getLastActivity());
        assertEquals(TIMEOUT_SECONDS, stats.getTimeoutSeconds());
        assertTrue(stats.getTimeUntilTimeout() > 0);
        assertFalse(stats.isTimedOut());
        assertEquals(0, stats.getTimeoutCount());
    }
    
    @Test
    void testTimeoutStatsForUnknownSession() {
        SessionTimeoutHandler.TimeoutStats stats = timeoutHandler.getTimeoutStats("UNKNOWN");
        assertNull(stats);
    }
    
    @Test
    void testMultipleSessions() {
        String session2Id = "SESSION_2";
        SessionTimeoutHandler.TimeoutCallback callback2 = mock(SessionTimeoutHandler.TimeoutCallback.class);
        
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        timeoutHandler.registerSession(session2Id, TIMEOUT_SECONDS, callback2);
        
        timeoutHandler.updateActivity(SESSION_ID);
        timeoutHandler.updateActivity(session2Id);
        
        assertNotNull(timeoutHandler.getTimeoutStats(SESSION_ID));
        assertNotNull(timeoutHandler.getTimeoutStats(session2Id));
        
        timeoutHandler.unregisterSession(SESSION_ID);
        timeoutHandler.unregisterSession(session2Id);
        
        assertNull(timeoutHandler.getTimeoutStats(SESSION_ID));
        assertNull(timeoutHandler.getTimeoutStats(session2Id));
    }
    
    @Test
    void testActivityResetTimeout() throws InterruptedException {
        timeoutHandler.registerSession(SESSION_ID, TIMEOUT_SECONDS, callback);
        
        // Wait almost until timeout
        Thread.sleep((TIMEOUT_SECONDS - 1) * 1000);
        
        // Update activity to reset timeout
        timeoutHandler.updateActivity(SESSION_ID);
        
        // Should not be timed out after activity update
        assertFalse(timeoutHandler.isTimedOut(SESSION_ID));
        
        // Time until timeout should be reset
        long timeUntil = timeoutHandler.getTimeUntilTimeout(SESSION_ID);
        assertTrue(timeUntil > TIMEOUT_SECONDS - 1);
    }
}