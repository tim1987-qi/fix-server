package com.fixserver.store.repository;

import com.fixserver.store.MessageStore;
import com.fixserver.store.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FIX message persistence
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    
    /**
     * Find messages by session ID and sequence number range
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.sessionId = :sessionId " +
           "AND m.direction = :direction " +
           "AND m.sequenceNumber BETWEEN :fromSeq AND :toSeq " +
           "ORDER BY m.sequenceNumber ASC")
    List<MessageEntity> findBySessionIdAndSequenceNumberRange(
        @Param("sessionId") String sessionId,
        @Param("direction") MessageStore.MessageDirection direction,
        @Param("fromSeq") int fromSeq,
        @Param("toSeq") int toSeq
    );
    
    /**
     * Find a specific message by session, sequence number and direction
     */
    Optional<MessageEntity> findBySessionIdAndSequenceNumberAndDirection(
        String sessionId, 
        Integer sequenceNumber, 
        MessageStore.MessageDirection direction
    );
    
    /**
     * Get the highest sequence number for a session and direction
     */
    @Query("SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM MessageEntity m " +
           "WHERE m.sessionId = :sessionId AND m.direction = :direction")
    Integer findMaxSequenceNumber(
        @Param("sessionId") String sessionId,
        @Param("direction") MessageStore.MessageDirection direction
    );
    
    /**
     * Find messages by session ID and timestamp range
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.sessionId = :sessionId " +
           "AND m.timestamp BETWEEN :fromTime AND :toTime " +
           "ORDER BY m.timestamp ASC")
    List<MessageEntity> findBySessionIdAndTimestampRange(
        @Param("sessionId") String sessionId,
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime
    );
    
    /**
     * Find all distinct session IDs
     */
    @Query("SELECT DISTINCT m.sessionId FROM MessageEntity m WHERE m.archivedAt IS NULL")
    List<String> findDistinctSessionIds();
    
    /**
     * Find messages older than specified date for archiving
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.timestamp < :beforeDate " +
           "AND m.archivedAt IS NULL ORDER BY m.timestamp ASC")
    List<MessageEntity> findMessagesForArchiving(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Count messages by session ID
     */
    long countBySessionId(String sessionId);
    
    /**
     * Count messages by session ID and direction
     */
    long countBySessionIdAndDirection(String sessionId, MessageStore.MessageDirection direction);
    
    /**
     * Find messages by message type
     */
    List<MessageEntity> findByMessageTypeOrderByTimestampDesc(String messageType);
    
    /**
     * Find recent messages for a session
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.sessionId = :sessionId " +
           "ORDER BY m.timestamp DESC")
    List<MessageEntity> findRecentMessagesBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * Delete archived messages older than specified date
     */
    @Query("DELETE FROM MessageEntity m WHERE m.archivedAt IS NOT NULL " +
           "AND m.archivedAt < :beforeDate")
    void deleteArchivedMessagesBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Mark messages as archived
     */
    @Query("UPDATE MessageEntity m SET m.archivedAt = :archivedAt " +
           "WHERE m.sessionId = :sessionId AND m.timestamp < :beforeDate " +
           "AND m.archivedAt IS NULL")
    int markMessagesAsArchived(
        @Param("sessionId") String sessionId,
        @Param("beforeDate") LocalDateTime beforeDate,
        @Param("archivedAt") LocalDateTime archivedAt
    );
}