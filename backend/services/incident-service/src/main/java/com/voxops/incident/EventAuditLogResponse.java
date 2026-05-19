package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record EventAuditLogResponse(
        UUID id,
        UUID eventId,
        String eventType,
        String topic,
        String messageKey,
        UUID aggregateId,
        String aggregateType,
        String payload,
        EventPublishStatus status,
        Integer kafkaPartition,
        Long kafkaOffset,
        String errorMessage,
        Instant occurredAt,
        Instant createdAt,
        Instant publishedAt
) {
    public static EventAuditLogResponse from(EventAuditLog log) {
        return new EventAuditLogResponse(
                log.getId(),
                log.getEventId(),
                log.getEventType(),
                log.getTopic(),
                log.getMessageKey(),
                log.getAggregateId(),
                log.getAggregateType(),
                log.getPayload(),
                log.getStatus(),
                log.getKafkaPartition(),
                log.getKafkaOffset(),
                log.getErrorMessage(),
                log.getOccurredAt(),
                log.getCreatedAt(),
                log.getPublishedAt()
        );
    }
}
