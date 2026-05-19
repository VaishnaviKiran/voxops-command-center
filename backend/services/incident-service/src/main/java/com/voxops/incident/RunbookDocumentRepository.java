package com.voxops.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RunbookDocumentRepository extends JpaRepository<RunbookDocument, UUID> {
}
