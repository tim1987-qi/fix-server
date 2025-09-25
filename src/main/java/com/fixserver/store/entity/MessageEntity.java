package com.fixserver.store.entity;

import com.fixserver.store.MessageStore;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for storing FIX messages
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_session_seq", columnList = "sessionId, sequenceNumber, direction"),
    @Index(name = "idx_session_time", columnList = "sessionId, timestamp"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String sessionId;
    
    @Column(nullable = false)
    private Integer sequenceNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStore.MessageDirection direction;
    
    @Column(nullable = false, length = 10)
    private String messageType;
    
    @Column(nullable = false, length = 100)
    private String senderCompId;
    
    @Column(nullable = false, length = 100)
    private String targetCompId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawMessage;
    
    @Column(length = 50)
    private String clientIpAddress;
    
    @Column(length = 500)
    private String errorMessage;
    
    @Column(nullable = false)
    private Boolean processed = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime archivedAt;
    
    public MessageEntity(String sessionId, Integer sequenceNumber, MessageStore.MessageDirection direction,
                        String messageType, String senderCompId, String targetCompId,
                        String rawMessage, String clientIpAddress) {
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.direction = direction;
        this.messageType = messageType;
        this.senderCompId = senderCompId;
        this.targetCompId = targetCompId;
        this.rawMessage = rawMessage;
        this.clientIpAddress = clientIpAddress;
        this.timestamp = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.processed = true;
    }
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}