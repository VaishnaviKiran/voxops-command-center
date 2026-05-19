package com.voxops.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "postmortems")
public class Postmortem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "generated_from", nullable = false, columnDefinition = "TEXT")
    private String generatedFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Postmortem() {
    }

    public Postmortem(Incident incident, String title, String content, String generatedFrom) {
        this.incident = incident;
        this.title = title;
        this.content = content;
        this.generatedFrom = generatedFrom;
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

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incident.getId();
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getGeneratedFrom() {
        return generatedFrom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
