package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {
    List<AiRecommendation> findByIncident_IdOrderByCreatedAtDesc(UUID incidentId);
}
