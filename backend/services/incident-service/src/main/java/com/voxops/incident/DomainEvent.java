package com.voxops.incident;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static DomainEvent of(
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload
    ) {
        return new DomainEvent(UUID.randomUUID(), eventType, aggregateId, aggregateType, Instant.now(), payload);
    }
}
