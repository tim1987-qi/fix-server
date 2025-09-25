package com.fixserver.store.entity;

import com.fixserver.store.MessageStore;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA entity for audit trail records
 */
@Entity
@Table(name = "audit_records", indexes = {
    @Index(name = "idx_audit_session", columnList = "sessionId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_type", columnList = "eventType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecordEntity implements MessageStore.AuditRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String sessionId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;
    
    @Column(length = 10)
    private String messageType;
    
    @Column(columnDefinition = "TEXT")
    private String rawMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageStore.MessageDirection direction;
    
    @Column(length = 50)
    private String clientIpAddress;
    
    @Column(length = 500)
    private String description;
    
    @ElementCollection
    @CollectionTable(name = "audit_additional_data", 
                    joinColumns = @JoinColumn(name = "audit_record_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> additionalData;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    public enum AuditEventType {
        SESSION_CREATED,
        SESSION_LOGON,
        SESSION_LOGOUT,
        SESSION_TIMEOUT,
        MESSAGE_RECEIVED,
        MESSAGE_SENT,
        MESSAGE_REJECTED,
        SEQUENCE_RESET,
        HEARTBEAT_TIMEOUT,
        TEST_REQUEST_SENT,
        RESEND_REQUEST,
        AUTHENTICATION_FAILURE,
        PROTOCOL_ERROR,
        SYSTEM_ERROR
    }
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.timestamp == null) {
            this.timestamp = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }
    
    public AuditRecordEntity(String sessionId, AuditEventType eventType, String description) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.description = description;
        LocalDateTime now = LocalDateTime.now();
        this.timestamp = now;
        this.createdAt = now;
    }
    
    public AuditRecordEntity(String sessionId, AuditEventType eventType, String messageType,
                           String rawMessage, MessageStore.MessageDirection direction,
                           String clientIpAddress, String description) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.messageType = messageType;
        this.rawMessage = rawMessage;
        this.direction = direction;
        this.clientIpAddress = clientIpAddress;
        this.description = description;
        LocalDateTime now = LocalDateTime.now();
        this.timestamp = now;
        this.createdAt = now;
    }
}