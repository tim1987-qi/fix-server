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

class GapFillManagerTest {
    
    @Mock
    private MessageStore messageStore;
    
    @Mock
    private FIXSession session;
    
    private GapFillManager gapFillManager;
    private static final String SESSION_ID = "TEST_SESSION";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(session.getSessionId()).thenReturn(SESSION_ID);
        when(session.sendMessage(any(FIXMessage.class))).thenReturn(CompletableFuture.completedFuture(null));
        
        gapFillManager = new GapFillManager(messageStore);
    }
    
    @Test
    void testDetectGapsNoGap() {
        List<GapFillManager.SequenceGap> gaps = gapFillManager.detectGaps(SESSION_ID, 5, 5);
        
        assertTrue(gaps.isEmpty());
    }
    
    @Test
    void testDetectGapsSingleGap() {
        List<GapFillManager.SequenceGap> gaps = gapFillManager.detectGaps(SESSION_ID, 3, 6);
        
        assertEquals(1, gaps.size());
        GapFillManager.SequenceGap gap = gaps.get(0);
        assertEquals(SESSION_ID, gap.getSessionId());
        assertEquals(3, gap.getBeginSeqNo());
        assertEquals(5, gap.getEndSeqNo());
        assertEquals(3, gap.getGapSize());
        assertFalse(gap.isResendRequested());
    }
    
    @Test
    void testDetectGapsBackwardsSequence() {
        // Received sequence is less than expected - no gap
        List<GapFillManager.SequenceGap> gaps = gapFillManager.detectGaps(SESSION_ID, 5, 3);
        
        assertTrue(gaps.isEmpty());
    }
    
    @Test
    void testRequestResend() {
        GapFillManager.SequenceGap gap = new GapFillManager.SequenceGap(SESSION_ID, 3, 5);
        
        CompletableFuture<Void> future = gapFillManager.requestResend(session, gap);
        
        assertDoesNotThrow(() -> future.join());
        assertTrue(gap.isResendRequested());
        assertNotNull(gap.getResendRequestTime());
        
        // Should send resend request message
        verify(session).sendMessage(argThat(message -> 
            "2".equals(message.getMessageType()) && // Resend Request
            "3".equals(message.getField(7)) && // BeginSeqNo
            "5".equals(message.getField(16)) // EndSeqNo
        ));
    }
    
    @Test
    void testProcessSequenceResetGapFill() {
        FIXMessage sequenceReset = createSequenceResetMessage(10, true);
        
        CompletableFuture<GapFillManager.SequenceResetResult> future = 
            gapFillManager.processSequenceReset(session, sequenceReset);
        
        GapFillManager.SequenceResetResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertTrue(result.isGapFill());
        assertEquals(10, result.getNewSeqNo());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void testProcessSequenceResetHard() {
        FIXMessage sequenceReset = createSequenceResetMessage(1, false);
        
        CompletableFuture<GapFillManager.SequenceResetResult> future = 
            gapFillManager.processSequenceReset(session, sequenceReset);
        
        GapFillManager.SequenceResetResult result = future.join();
        
        assertTrue(result.isSuccess());
        assertFalse(result.isGapFill());
        assertEquals(1, result.getNewSeqNo());
        assertNull(result.getErrorMessage());
    }
    
    @Test
    void testProcessSequenceResetMissingNewSeqNo() {
        FIXMessage sequenceReset = new FIXMessageImpl("FIX.4.4", "4");
        // Missing NewSeqNo field
        
        CompletableFuture<GapFillManager.SequenceResetResult> future = 
            gapFillManager.processSequenceReset(session, sequenceReset);
        
        GapFillManager.SequenceResetResult result = future.join();
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Missing NewSeqNo"));
    }
    
    @Test
    void testProcessSequenceResetInvalidNewSeqNo() {
        FIXMessage sequenceReset = new FIXMessageImpl("FIX.4.4", "4");
        sequenceReset.setField(36, "INVALID"); // Invalid NewSeqNo
        
        CompletableFuture<GapFillManager.SequenceResetResult> future = 
            gapFillManager.processSequenceReset(session, sequenceReset);
        
        GapFillManager.SequenceResetResult result = future.join();
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Invalid NewSeqNo"));
    }
    
    @Test
    void testFillsGap() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 5),
            new GapFillManager.SequenceGap(SESSION_ID, 8, 10)
        );
        
        assertTrue(gapFillManager.fillsGap(gaps, 4)); // In first gap
        assertTrue(gapFillManager.fillsGap(gaps, 9)); // In second gap
        assertFalse(gapFillManager.fillsGap(gaps, 2)); // Before gaps
        assertFalse(gapFillManager.fillsGap(gaps, 7)); // Between gaps
        assertFalse(gapFillManager.fillsGap(gaps, 12)); // After gaps
    }
    
    @Test
    void testUpdateGapsCompletelyFilled() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 5, 5) // Single message gap
        );
        
        List<GapFillManager.SequenceGap> updatedGaps = gapFillManager.updateGaps(gaps, 5);
        
        assertTrue(updatedGaps.isEmpty()); // Gap should be completely filled
    }
    
    @Test
    void testUpdateGapsFilledAtBeginning() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 6)
        );
        
        List<GapFillManager.SequenceGap> updatedGaps = gapFillManager.updateGaps(gaps, 3);
        
        assertEquals(1, updatedGaps.size());
        GapFillManager.SequenceGap remainingGap = updatedGaps.get(0);
        assertEquals(4, remainingGap.getBeginSeqNo());
        assertEquals(6, remainingGap.getEndSeqNo());
    }
    
    @Test
    void testUpdateGapsFilledAtEnd() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 6)
        );
        
        List<GapFillManager.SequenceGap> updatedGaps = gapFillManager.updateGaps(gaps, 6);
        
        assertEquals(1, updatedGaps.size());
        GapFillManager.SequenceGap remainingGap = updatedGaps.get(0);
        assertEquals(3, remainingGap.getBeginSeqNo());
        assertEquals(5, remainingGap.getEndSeqNo());
    }
    
    @Test
    void testUpdateGapsFilledInMiddle() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 7)
        );
        
        List<GapFillManager.SequenceGap> updatedGaps = gapFillManager.updateGaps(gaps, 5);
        
        assertEquals(2, updatedGaps.size());
        
        GapFillManager.SequenceGap beforeGap = updatedGaps.get(0);
        assertEquals(3, beforeGap.getBeginSeqNo());
        assertEquals(4, beforeGap.getEndSeqNo());
        
        GapFillManager.SequenceGap afterGap = updatedGaps.get(1);
        assertEquals(6, afterGap.getBeginSeqNo());
        assertEquals(7, afterGap.getEndSeqNo());
    }
    
    @Test
    void testUpdateGapsNotAffected() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 5),
            new GapFillManager.SequenceGap(SESSION_ID, 8, 10)
        );
        
        List<GapFillManager.SequenceGap> updatedGaps = gapFillManager.updateGaps(gaps, 12);
        
        assertEquals(2, updatedGaps.size()); // Both gaps should remain unchanged
        assertEquals(gaps.get(0).getBeginSeqNo(), updatedGaps.get(0).getBeginSeqNo());
        assertEquals(gaps.get(1).getBeginSeqNo(), updatedGaps.get(1).getBeginSeqNo());
    }
    
    @Test
    void testGetGapStatistics() {
        List<GapFillManager.SequenceGap> gaps = Arrays.asList(
            new GapFillManager.SequenceGap(SESSION_ID, 3, 5), // 3 messages
            new GapFillManager.SequenceGap(SESSION_ID, 8, 10) // 3 messages
        );
        
        // Mark one gap as having resend requested
        gaps.get(0).setResendRequested(true);
        
        GapFillManager.GapStatistics stats = gapFillManager.getGapStatistics(SESSION_ID, gaps);
        
        assertEquals(SESSION_ID, stats.getSessionId());
        assertEquals(2, stats.getTotalGaps());
        assertEquals(6, stats.getTotalMissingMessages()); // 3 + 3
        assertEquals(1, stats.getPendingResends());
    }
    
    @Test
    void testSequenceGapToString() {
        GapFillManager.SequenceGap gap = new GapFillManager.SequenceGap(SESSION_ID, 3, 7);
        gap.setResendRequested(true);
        
        String toString = gap.toString();
        assertTrue(toString.contains(SESSION_ID));
        assertTrue(toString.contains("3-7"));
        assertTrue(toString.contains("size=5"));
        assertTrue(toString.contains("requested=true"));
    }
    
    @Test
    void testSequenceResetResultStaticMethods() {
        GapFillManager.SequenceResetResult gapFillResult = 
            GapFillManager.SequenceResetResult.gapFill(10);
        assertTrue(gapFillResult.isSuccess());
        assertTrue(gapFillResult.isGapFill());
        assertEquals(10, gapFillResult.getNewSeqNo());
        
        GapFillManager.SequenceResetResult hardResetResult = 
            GapFillManager.SequenceResetResult.hardReset(1);
        assertTrue(hardResetResult.isSuccess());
        assertFalse(hardResetResult.isGapFill());
        assertEquals(1, hardResetResult.getNewSeqNo());
        
        GapFillManager.SequenceResetResult errorResult = 
            GapFillManager.SequenceResetResult.error("Test error");
        assertFalse(errorResult.isSuccess());
        assertEquals("Test error", errorResult.getErrorMessage());
    }
    
    private FIXMessage createSequenceResetMessage(int newSeqNo, boolean gapFill) {
        FIXMessage sequenceReset = new FIXMessageImpl("FIX.4.4", "4"); // Sequence Reset
        sequenceReset.setField(36, String.valueOf(newSeqNo)); // NewSeqNo
        sequenceReset.setField(123, gapFill ? "Y" : "N"); // GapFillFlag
        return sequenceReset;
    }
}