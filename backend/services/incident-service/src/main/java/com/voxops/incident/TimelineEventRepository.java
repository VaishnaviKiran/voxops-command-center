package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {
    List<TimelineEvent> findByIncident_IdOrderByCreatedAtDesc(UUID incidentId);

    List<TimelineEvent> findTop15ByOrderByCreatedAtDesc();
}
