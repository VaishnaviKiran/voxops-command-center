package com.voxops.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "transcript_segments")
public class TranscriptSegment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "speaker_label", nullable = false, length = 120)
    private String speakerLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TranscriptSegment() {
    }

    public TranscriptSegment(Incident incident, String speakerLabel, String text, BigDecimal confidence) {
        this.incident = incident;
        this.speakerLabel = speakerLabel;
        this.text = text;
        this.confidence = confidence;
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

    public String getSpeakerLabel() {
        return speakerLabel;
    }

    public String getText() {
        return text;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
