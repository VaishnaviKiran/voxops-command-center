package com.voxops.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String citations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiRecommendationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiRecommendation() {
    }

    public AiRecommendation(Incident incident, String prompt, String response, BigDecimal confidence, String citations) {
        this.incident = incident;
        this.prompt = prompt;
        this.response = response;
        this.confidence = confidence;
        this.citations = citations;
        this.status = AiRecommendationStatus.GENERATED;
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

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getCitations() {
        return citations;
    }

    public AiRecommendationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
