package com.fixserver.replay;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.session.FIXSession;
import com.fixserver.store.MessageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class MessageReplayServiceTest {
    
    @Mock
    private MessageStore messageStore;
    
    @Mock
    private FIXSession session;
    
    private MessageReplayService replayService;
    private static final String SESSION_ID = "TEST_SESSION";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(session.getSessionId()).thenReturn(SESSION_ID);
        when(session.sendMessage(any(FIXMessage.class))).thenReturn(CompletableFuture.completedFuture(null));
        
        replayService = new MessageReplayService(messageStore);
    }
    
    @Test
    void testHandleResendRequestSuccess() {
        // Setup test data
        List<FIXMessage> messages = Arrays.asList(
            createTestMessage(1, "D"),
            createTestMessage(2, "8"),
            createTestMessage(3, "F")
        );
        
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        when(messageStore.getMessages(SESSION_ID, 1, 3))
            .thenReturn(messages);
        
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.handleResendRequest(session, 1, 3);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getMessagesReplayed());
        assertEquals(0, result.getGapFillsSent());
        assertEquals(1, result.getBeginSeqNo());
        assertEquals(3, result.getEndSeqNo());
        
        verify(session, times(3)).sendMessage(any(FIXMessage.class));
        verify(messageStore).getMessages(SESSION_ID, 1, 3);
    }
    
    @Test
    void testHandleResendRequestWithGaps() {
        // Setup test data with gaps (missing sequence 2)
        List<FIXMessage> messages = Arrays.asList(
            createTestMessage(1, "D"),
            createTestMessage(3, "F")
        );
        
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        when(messageStore.getMessages(SESSION_ID, 1, 3))
            .thenReturn(messages);
        
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.handleResendRequest(session, 1, 3);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getMessagesReplayed()); // Messages 1 and 3
        assertEquals(1, result.getGapFillsSent()); // Gap fill for sequence 2
        
        // Should send 2 actual messages + 1 gap fill = 3 total
        verify(session, times(3)).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testHandleResendRequestInvalidRange() {
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.handleResendRequest(session, 0, 5);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Invalid sequence number range"));
        
        verify(session, never()).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testHandleResendRequestBeyondLastSent() {
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.handleResendRequest(session, 10, 15);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Begin sequence number beyond last sent"));
    }
    
    @Test
    void testHandleResendRequestInfiniteEnd() {
        List<FIXMessage> messages = Arrays.asList(
            createTestMessage(3, "D"),
            createTestMessage(4, "8"),
            createTestMessage(5, "F")
        );
        
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        when(messageStore.getMessages(SESSION_ID, 3, 5))
            .thenReturn(messages);
        
        // EndSeqNo = 0 means infinity
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.handleResendRequest(session, 3, 0);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getMessagesReplayed());
        assertEquals(5, result.getEndSeqNo()); // Should be limited to last sent
        
        verify(messageStore).getMessages(SESSION_ID, 3, 5);
    }
    
    @Test
    void testReplayMessagesForRecovery() {
        List<FIXMessage> messages = Arrays.asList(
            createTestMessage(2, "D"),
            createTestMessage(3, "8")
        );
        
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(3);
        when(messageStore.getMessages(SESSION_ID, 2, 3))
            .thenReturn(messages);
        
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.replayMessagesForRecovery(session, 2);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getMessagesReplayed());
        assertEquals(0, result.getGapFillsSent());
        
        verify(session, times(2)).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testReplayMessagesForRecoveryNoMessages() {
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        
        CompletableFuture<MessageReplayService.ReplayResult> future = 
            replayService.replayMessagesForRecovery(session, 10);
        
        MessageReplayService.ReplayResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertEquals(0, result.getMessagesReplayed());
        
        verify(session, never()).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testHandleSequenceResetGapFill() {
        CompletableFuture<Void> future = 
            replayService.handleSequenceReset(session, 10, true);
        
        assertDoesNotThrow(() -> future.join());
        
        // Should send sequence reset message
        verify(session).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testHandleSequenceResetHard() {
        CompletableFuture<Void> future = 
            replayService.handleSequenceReset(session, 1, false);
        
        assertDoesNotThrow(() -> future.join());
        
        // Should send sequence reset message
        verify(session).sendMessage(any(FIXMessage.class));
    }
    
    @Test
    void testGetReplayStatistics() {
        MessageReplayService.ReplayStatistics stats = replayService.getReplayStatistics(SESSION_ID);
        
        assertNotNull(stats);
        assertEquals(SESSION_ID, stats.getSessionId());
        assertEquals(0, stats.getTotalReplays());
        assertEquals(0, stats.getTotalMessagesReplayed());
        assertEquals(0, stats.getTotalGapFills());
    }
    
    @Test
    void testReplayResultSuccess() {
        MessageReplayService.ReplayResult result = 
            MessageReplayService.ReplayResult.success(5, 2, 1, 10);
        
        assertTrue(result.isSuccess());
        assertNull(result.getErrorMessage());
        assertEquals(5, result.getMessagesReplayed());
        assertEquals(2, result.getGapFillsSent());
        assertEquals(1, result.getBeginSeqNo());
        assertEquals(10, result.getEndSeqNo());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains("replayed=5"));
        assertTrue(toString.contains("gapFills=2"));
    }
    
    @Test
    void testReplayResultError() {
        MessageReplayService.ReplayResult result = 
            MessageReplayService.ReplayResult.error("Test error");
        
        assertFalse(result.isSuccess());
        assertEquals("Test error", result.getErrorMessage());
        assertEquals(0, result.getMessagesReplayed());
        assertEquals(0, result.getGapFillsSent());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains("Test error"));
    }
    
    @Test
    void testMessageReplayWithPossDupFlag() {
        List<FIXMessage> messages = Arrays.asList(createTestMessage(1, "D"));
        
        when(messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        when(messageStore.getMessages(SESSION_ID, 1, 1))
            .thenReturn(messages);
        
        replayService.handleResendRequest(session, 1, 1).join();
        
        // Verify that PossDupFlag was set on the replayed message
        verify(session).sendMessage(argThat(message -> 
            "Y".equals(message.getField(43)) // PossDupFlag
        ));
    }
    
    private FIXMessage createTestMessage(int sequenceNumber, String messageType) {
        FIXMessageImpl message = new FIXMessageImpl("FIX.4.4", messageType);
        message.setField(FIXMessage.SENDER_COMP_ID, "SENDER");
        message.setField(FIXMessage.TARGET_COMP_ID, "TARGET");
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, String.valueOf(sequenceNumber));
        return message;
    }
}