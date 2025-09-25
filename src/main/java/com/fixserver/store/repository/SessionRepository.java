package com.fixserver.store.repository;

import com.fixserver.session.FIXSession;
import com.fixserver.store.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FIX session persistence
 */
@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    
    /**
     * Find session by session ID
     */
    Optional<SessionEntity> findBySessionId(String sessionId);
    
    /**
     * Find sessions by sender and target company IDs
     */
    List<SessionEntity> findBySenderCompIdAndTargetCompId(String senderCompId, String targetCompId);
    
    /**
     * Find sessions by status
     */
    List<SessionEntity> findByStatus(FIXSession.Status status);
    
    /**
     * Find active sessions (not disconnected)
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.status != 'DISCONNECTED' " +
           "AND s.archivedAt IS NULL")
    List<SessionEntity> findActiveSessions();
    
    /**
     * Find sessions that haven't had a heartbeat within the specified time
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.lastHeartbeat < :cutoffTime " +
           "AND s.status = 'LOGGED_ON' AND s.archivedAt IS NULL")
    List<SessionEntity> findSessionsWithOldHeartbeat(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find sessions by sender company ID
     */
    List<SessionEntity> findBySenderCompId(String senderCompId);
    
    /**
     * Find sessions by target company ID
     */
    List<SessionEntity> findByTargetCompId(String targetCompId);
    
    /**
     * Count active sessions
     */
    @Query("SELECT COUNT(s) FROM SessionEntity s WHERE s.status != 'DISCONNECTED' " +
           "AND s.archivedAt IS NULL")
    long countActiveSessions();
    
    /**
     * Count sessions by status
     */
    long countByStatus(FIXSession.Status status);
    
    /**
     * Find sessions created within time range
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.createdAt BETWEEN :fromTime AND :toTime " +
           "ORDER BY s.createdAt DESC")
    List<SessionEntity> findSessionsCreatedBetween(
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime
    );
    
    /**
     * Find sessions for archiving (old and disconnected)
     */
    @Query("SELECT s FROM SessionEntity s WHERE s.status = 'DISCONNECTED' " +
           "AND s.updatedAt < :cutoffTime AND s.archivedAt IS NULL")
    List<SessionEntity> findSessionsForArchiving(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Mark sessions as archived
     */
    @Query("UPDATE SessionEntity s SET s.archivedAt = :archivedAt " +
           "WHERE s.status = 'DISCONNECTED' AND s.updatedAt < :cutoffTime " +
           "AND s.archivedAt IS NULL")
    int markSessionsAsArchived(
        @Param("cutoffTime") LocalDateTime cutoffTime,
        @Param("archivedAt") LocalDateTime archivedAt
    );
    
    /**
     * Delete archived sessions older than specified date
     */
    @Query("DELETE FROM SessionEntity s WHERE s.archivedAt IS NOT NULL " +
           "AND s.archivedAt < :beforeDate")
    void deleteArchivedSessionsBefore(@Param("beforeDate") LocalDateTime beforeDate);
}