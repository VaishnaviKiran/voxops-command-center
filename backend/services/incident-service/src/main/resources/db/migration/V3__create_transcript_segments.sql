CREATE TABLE transcript_segments (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    speaker_label VARCHAR(120) NOT NULL,
    text TEXT NOT NULL,
    confidence NUMERIC(5, 4),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transcript_segments_incident_created_at
    ON transcript_segments (incident_id, created_at DESC);
