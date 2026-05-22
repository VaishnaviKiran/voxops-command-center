package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, UUID> {
    List<EventAuditLog> findByAggregateIdOrderByCreatedAtDesc(UUID aggregateId);

    List<EventAuditLog> findTop20ByOrderByCreatedAtDesc();

    long countByStatus(EventPublishStatus status);

    void deleteByAggregateId(UUID aggregateId);
}
