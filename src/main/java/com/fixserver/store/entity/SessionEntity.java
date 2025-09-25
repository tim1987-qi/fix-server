package com.fixserver.store.entity;

import com.fixserver.session.FIXSession;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for storing FIX session state
 */
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId", unique = true),
    @Index(name = "idx_sender_target", columnList = "senderCompId, targetCompId"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String sessionId;
    
    @Column(nullable = false, length = 100)
    private String senderCompId;
    
    @Column(nullable = false, length = 100)
    private String targetCompId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FIXSession.Status status;
    
    @Column(nullable = false)
    private Integer incomingSequenceNumber = 1;
    
    @Column(nullable = false)
    private Integer outgoingSequenceNumber = 1;
    
    @Column
    private LocalDateTime lastHeartbeat;
    
    @Column
    private LocalDateTime sessionStartTime;
    
    @Column
    private Integer heartbeatInterval = 30;
    
    @Column(length = 500)
    private String lastError;
    
    @Column(nullable = false)
    private Long totalMessagesReceived = 0L;
    
    @Column(nullable = false)
    private Long totalMessagesSent = 0L;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime archivedAt;
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public SessionEntity(String sessionId, String senderCompId, String targetCompId) {
        this.sessionId = sessionId;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.status = FIXSession.Status.DISCONNECTED;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}