package com.fixserver.store.repository;

import com.fixserver.store.entity.AuditRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit record persistence
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditRecordEntity, Long> {
    
    /**
     * Find audit records by session ID and time range
     */
    @Query("SELECT a FROM AuditRecordEntity a WHERE a.sessionId = :sessionId " +
           "AND a.timestamp BETWEEN :fromTime AND :toTime " +
           "ORDER BY a.timestamp ASC")
    List<AuditRecordEntity> findBySessionIdAndTimestampRange(
        @Param("sessionId") String sessionId,
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime
    );
    
    /**
     * Find audit records by event type
     */
    List<AuditRecordEntity> findByEventTypeOrderByTimestampDesc(
        AuditRecordEntity.AuditEventType eventType
    );
    
    /**
     * Find audit records by session ID
     */
    List<AuditRecordEntity> findBySessionIdOrderByTimestampDesc(String sessionId);
    
    /**
     * Find recent audit records
     */
    @Query("SELECT a FROM AuditRecordEntity a WHERE a.timestamp >= :since " +
           "ORDER BY a.timestamp DESC")
    List<AuditRecordEntity> findRecentAuditRecords(@Param("since") LocalDateTime since);
    
    /**
     * Find audit records by message type
     */
    List<AuditRecordEntity> findByMessageTypeOrderByTimestampDesc(String messageType);
    
    /**
     * Count audit records by session ID
     */
    long countBySessionId(String sessionId);
    
    /**
     * Count audit records by event type
     */
    long countByEventType(AuditRecordEntity.AuditEventType eventType);
    
    /**
     * Find audit records for archiving
     */
    @Query("SELECT a FROM AuditRecordEntity a WHERE a.timestamp < :cutoffTime " +
           "ORDER BY a.timestamp ASC")
    List<AuditRecordEntity> findAuditRecordsForArchiving(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Delete audit records older than specified date
     */
    @Query("DELETE FROM AuditRecordEntity a WHERE a.timestamp < :beforeDate")
    void deleteAuditRecordsBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Find error audit records
     */
    @Query("SELECT a FROM AuditRecordEntity a WHERE a.eventType IN " +
           "('AUTHENTICATION_FAILURE', 'PROTOCOL_ERROR', 'SYSTEM_ERROR', 'MESSAGE_REJECTED') " +
           "ORDER BY a.timestamp DESC")
    List<AuditRecordEntity> findErrorAuditRecords();
    
    /**
     * Find session lifecycle audit records
     */
    @Query("SELECT a FROM AuditRecordEntity a WHERE a.sessionId = :sessionId " +
           "AND a.eventType IN ('SESSION_CREATED', 'SESSION_LOGON', 'SESSION_LOGOUT', 'SESSION_TIMEOUT') " +
           "ORDER BY a.timestamp ASC")
    List<AuditRecordEntity> findSessionLifecycleAuditRecords(@Param("sessionId") String sessionId);
}