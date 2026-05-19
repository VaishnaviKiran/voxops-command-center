package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostmortemRepository extends JpaRepository<Postmortem, UUID> {
    List<Postmortem> findByIncident_IdOrderByCreatedAtDesc(UUID incidentId);
}
