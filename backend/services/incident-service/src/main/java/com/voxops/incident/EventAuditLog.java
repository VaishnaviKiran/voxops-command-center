package com.voxops.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_audit_log")
public class EventAuditLog {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 160)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 120)
    private String messageKey;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventPublishStatus status;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected EventAuditLog() {
    }

    public EventAuditLog(String topic, String messageKey, DomainEvent event, String payload, EventPublishStatus status) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.topic = topic;
        this.messageKey = messageKey;
        this.aggregateId = event.aggregateId();
        this.aggregateType = event.aggregateType();
        this.payload = payload;
        this.status = status;
        this.occurredAt = event.occurredAt();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markPublished(int partition, long offset) {
        this.status = EventPublishStatus.PUBLISHED;
        this.kafkaPartition = partition;
        this.kafkaOffset = offset;
        this.publishedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = EventPublishStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getPayload() {
        return payload;
    }

    public EventPublishStatus getStatus() {
        return status;
    }

    public Integer getKafkaPartition() {
        return kafkaPartition;
    }

    public Long getKafkaOffset() {
        return kafkaOffset;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
