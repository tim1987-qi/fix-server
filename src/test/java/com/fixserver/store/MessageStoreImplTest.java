package com.fixserver.store;

import com.fixserver.core.FIXMessage;
import com.fixserver.core.FIXMessageImpl;
import com.fixserver.protocol.FIXProtocolHandler;
import com.fixserver.store.entity.AuditRecordEntity;
import com.fixserver.store.entity.MessageEntity;
import com.fixserver.store.repository.AuditRepository;
import com.fixserver.store.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class MessageStoreImplTest {
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private AuditRepository auditRepository;
    
    @Mock
    private FIXProtocolHandler protocolHandler;
    
    private MessageStoreImpl messageStore;
    private static final String SESSION_ID = "TEST_SESSION";
    private static final String SENDER_COMP_ID = "SENDER";
    private static final String TARGET_COMP_ID = "TARGET";
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageStore = new MessageStoreImpl(messageRepository, auditRepository, protocolHandler);
    }
    
    @Test
    void testStoreMessage() {
        FIXMessage message = createTestMessage();
        String rawMessage = "8=FIX.4.4|35=D|49=SENDER|56=TARGET|34=1|";
        
        when(protocolHandler.format(message)).thenReturn(rawMessage);
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(new MessageEntity());
        when(auditRepository.save(any(AuditRecordEntity.class))).thenReturn(new AuditRecordEntity());
        
        assertDoesNotThrow(() -> 
            messageStore.storeMessage(SESSION_ID, message, MessageStore.MessageDirection.OUTGOING));
        
        verify(messageRepository).save(any(MessageEntity.class));
        verify(auditRepository).save(any(AuditRecordEntity.class));
        verify(protocolHandler).format(message);
    }
    
    @Test
    void testStoreMessageFailure() {
        FIXMessage message = createTestMessage();
        
        when(protocolHandler.format(message)).thenThrow(new RuntimeException("Format error"));
        
        assertThrows(MessageStoreImpl.MessageStoreException.class, () ->
            messageStore.storeMessage(SESSION_ID, message, MessageStore.MessageDirection.OUTGOING));
    }
    
    @Test
    void testGetMessages() {
        MessageEntity entity1 = createTestMessageEntity(1);
        MessageEntity entity2 = createTestMessageEntity(2);
        List<MessageEntity> entities = Arrays.asList(entity1, entity2);
        
        when(messageRepository.findBySessionIdAndSequenceNumberRange(
            SESSION_ID, MessageStore.MessageDirection.OUTGOING, 1, 2))
            .thenReturn(entities);
        
        when(protocolHandler.parse(anyString())).thenReturn(createTestMessage());
        
        List<FIXMessage> messages = messageStore.getMessages(SESSION_ID, 1, 2);
        
        assertEquals(2, messages.size());
        verify(messageRepository).findBySessionIdAndSequenceNumberRange(
            SESSION_ID, MessageStore.MessageDirection.OUTGOING, 1, 2);
    }
    
    @Test
    void testGetMessage() {
        MessageEntity entity = createTestMessageEntity(1);
        
        when(messageRepository.findBySessionIdAndSequenceNumberAndDirection(
            SESSION_ID, 1, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(Optional.of(entity));
        
        when(protocolHandler.parse(anyString())).thenReturn(createTestMessage());
        
        Optional<FIXMessage> message = messageStore.getMessage(
            SESSION_ID, 1, MessageStore.MessageDirection.OUTGOING);
        
        assertTrue(message.isPresent());
        verify(messageRepository).findBySessionIdAndSequenceNumberAndDirection(
            SESSION_ID, 1, MessageStore.MessageDirection.OUTGOING);
    }
    
    @Test
    void testGetMessageNotFound() {
        when(messageRepository.findBySessionIdAndSequenceNumberAndDirection(
            SESSION_ID, 1, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(Optional.empty());
        
        Optional<FIXMessage> message = messageStore.getMessage(
            SESSION_ID, 1, MessageStore.MessageDirection.OUTGOING);
        
        assertFalse(message.isPresent());
    }
    
    @Test
    void testGetLastSequenceNumber() {
        when(messageRepository.findMaxSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(5);
        
        int lastSeqNum = messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING);
        
        assertEquals(5, lastSeqNum);
        verify(messageRepository).findMaxSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING);
    }
    
    @Test
    void testGetLastSequenceNumberNoMessages() {
        when(messageRepository.findMaxSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(null);
        
        int lastSeqNum = messageStore.getLastSequenceNumber(SESSION_ID, MessageStore.MessageDirection.OUTGOING);
        
        assertEquals(0, lastSeqNum);
    }
    
    @Test
    void testArchiveMessages() {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(30);
        
        when(messageRepository.markMessagesAsArchived(eq(SESSION_ID), eq(beforeDate), any(LocalDateTime.class)))
            .thenReturn(10);
        when(auditRepository.save(any(AuditRecordEntity.class))).thenReturn(new AuditRecordEntity());
        
        assertDoesNotThrow(() -> messageStore.archiveMessages(SESSION_ID, beforeDate));
        
        verify(messageRepository).markMessagesAsArchived(eq(SESSION_ID), eq(beforeDate), any(LocalDateTime.class));
        verify(auditRepository).save(any(AuditRecordEntity.class));
    }
    
    @Test
    void testGetAuditTrail() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now();
        
        AuditRecordEntity auditEntity = new AuditRecordEntity();
        auditEntity.setSessionId(SESSION_ID);
        auditEntity.setEventType(AuditRecordEntity.AuditEventType.MESSAGE_SENT);
        auditEntity.setTimestamp(LocalDateTime.now());
        
        when(auditRepository.findBySessionIdAndTimestampRange(SESSION_ID, from, to))
            .thenReturn(Arrays.asList(auditEntity));
        
        List<MessageStore.AuditRecord> auditTrail = messageStore.getAuditTrail(SESSION_ID, from, to);
        
        assertEquals(1, auditTrail.size());
        verify(auditRepository).findBySessionIdAndTimestampRange(SESSION_ID, from, to);
    }
    
    @Test
    void testGetActiveSessions() {
        List<String> sessionIds = Arrays.asList("SESSION1", "SESSION2", "SESSION3");
        
        when(messageRepository.findDistinctSessionIds()).thenReturn(sessionIds);
        
        List<String> activeSessions = messageStore.getActiveSessions();
        
        assertEquals(3, activeSessions.size());
        assertTrue(activeSessions.contains("SESSION1"));
        verify(messageRepository).findDistinctSessionIds();
    }
    
    @Test
    void testClearSession() {
        MessageEntity messageEntity = createTestMessageEntity(1);
        AuditRecordEntity auditEntity = new AuditRecordEntity();
        
        when(messageRepository.findRecentMessagesBySessionId(SESSION_ID))
            .thenReturn(Arrays.asList(messageEntity));
        when(auditRepository.findBySessionIdOrderByTimestampDesc(SESSION_ID))
            .thenReturn(Arrays.asList(auditEntity));
        
        assertDoesNotThrow(() -> messageStore.clearSession(SESSION_ID));
        
        verify(messageRepository).deleteAll(Arrays.asList(messageEntity));
        verify(auditRepository).deleteAll(Arrays.asList(auditEntity));
    }
    
    @Test
    void testGetMessageStatistics() {
        when(messageRepository.countBySessionId(SESSION_ID)).thenReturn(100L);
        when(messageRepository.countBySessionIdAndDirection(SESSION_ID, MessageStore.MessageDirection.INCOMING))
            .thenReturn(60L);
        when(messageRepository.countBySessionIdAndDirection(SESSION_ID, MessageStore.MessageDirection.OUTGOING))
            .thenReturn(40L);
        
        MessageStoreImpl.MessageStatistics stats = messageStore.getMessageStatistics(SESSION_ID);
        
        assertEquals(SESSION_ID, stats.getSessionId());
        assertEquals(100L, stats.getTotalMessages());
        assertEquals(60L, stats.getIncomingMessages());
        assertEquals(40L, stats.getOutgoingMessages());
    }
    
    @Test
    void testCreateAuditRecord() {
        when(auditRepository.save(any(AuditRecordEntity.class))).thenReturn(new AuditRecordEntity());
        
        assertDoesNotThrow(() -> messageStore.createAuditRecord(
            SESSION_ID, AuditRecordEntity.AuditEventType.SESSION_LOGON, "Test description"));
        
        verify(auditRepository).save(any(AuditRecordEntity.class));
    }
    
    @Test
    void testCleanupArchivedMessages() {
        LocalDateTime beforeDate = LocalDateTime.now().minusMonths(6);
        
        assertDoesNotThrow(() -> messageStore.cleanupArchivedMessages(beforeDate));
        
        verify(messageRepository).deleteArchivedMessagesBefore(beforeDate);
        verify(auditRepository).deleteAuditRecordsBefore(beforeDate);
    }
    
    @Test
    void testConvertToFIXMessageWithParseError() {
        MessageEntity entity = createTestMessageEntity(1);
        
        when(protocolHandler.parse(entity.getRawMessage()))
            .thenThrow(new RuntimeException("Parse error"));
        
        // This should be called internally when getting messages
        when(messageRepository.findBySessionIdAndSequenceNumberRange(
            SESSION_ID, MessageStore.MessageDirection.OUTGOING, 1, 1))
            .thenReturn(Arrays.asList(entity));
        
        List<FIXMessage> messages = messageStore.getMessages(SESSION_ID, 1, 1);
        
        assertEquals(1, messages.size());
        FIXMessage message = messages.get(0);
        assertEquals("D", message.getMessageType());
        assertEquals(SENDER_COMP_ID, message.getSenderCompId());
        assertEquals(TARGET_COMP_ID, message.getTargetCompId());
    }
    
    private FIXMessage createTestMessage() {
        FIXMessageImpl message = new FIXMessageImpl("FIX.4.4", "D");
        message.setField(FIXMessage.SENDER_COMP_ID, SENDER_COMP_ID);
        message.setField(FIXMessage.TARGET_COMP_ID, TARGET_COMP_ID);
        message.setField(FIXMessage.MESSAGE_SEQUENCE_NUMBER, "1");
        return message;
    }
    
    private MessageEntity createTestMessageEntity(int sequenceNumber) {
        return new MessageEntity(
            SESSION_ID,
            sequenceNumber,
            MessageStore.MessageDirection.OUTGOING,
            "D",
            SENDER_COMP_ID,
            TARGET_COMP_ID,
            "8=FIX.4.4|35=D|49=SENDER|56=TARGET|34=" + sequenceNumber + "|",
            "127.0.0.1"
        );
    }
}