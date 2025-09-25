package com.fixserver;

import com.fixserver.store.MessageStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test for FIX Server Application startup.
 * 
 * This test verifies that the application can start successfully
 * without database dependencies, using the in-memory message store.
 */
@SpringBootTest
@ActiveProfiles("test")
class FIXServerApplicationTest {
    
    @Autowired
    private MessageStore messageStore;
    
    /**
     * Test that the Spring context loads successfully.
     * This verifies that all beans can be created and autowired correctly.
     */
    @Test
    void contextLoads() {
        // If we get here, the Spring context loaded successfully
        assertNotNull(messageStore, "MessageStore should be available");
    }
    
    /**
     * Test that the MessageStore is working (basic functionality).
     */
    @Test
    void messageStoreIsWorking() {
        // Basic test to ensure MessageStore is functional
        assertNotNull(messageStore);
        
        // Test getting active sessions (should be empty initially)
        List<String> activeSessions = messageStore.getActiveSessions();
        assertNotNull(activeSessions);
        
        // Test getting last sequence number for non-existent session
        int lastSeqNum = messageStore.getLastSequenceNumber("test-session", MessageStore.MessageDirection.OUTGOING);
        assertEquals(0, lastSeqNum);
    }
}