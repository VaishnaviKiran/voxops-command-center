package com.voxops.incident;

import java.time.Instant;
import java.util.UUID;

public record DashboardRecentDomainEvent(
        UUID incidentId,
        String eventType,
        String topic,
        EventPublishStatus status,
        Instant createdAt
) {
    public static DashboardRecentDomainEvent from(EventAuditLog log) {
        return new DashboardRecentDomainEvent(
                log.getAggregateId(),
                log.getEventType(),
                log.getTopic(),
                log.getStatus(),
                log.getCreatedAt()
        );
    }
}
