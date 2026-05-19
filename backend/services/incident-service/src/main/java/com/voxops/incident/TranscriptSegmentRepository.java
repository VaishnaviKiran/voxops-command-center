package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, UUID> {
    List<TranscriptSegment> findByIncident_IdOrderByCreatedAtDesc(UUID incidentId);
}
