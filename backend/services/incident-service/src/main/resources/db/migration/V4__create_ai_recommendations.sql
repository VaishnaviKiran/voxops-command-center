CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    prompt TEXT NOT NULL,
    response TEXT NOT NULL,
    confidence NUMERIC(5, 4),
    citations TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ai_recommendations_incident_created_at
    ON ai_recommendations (incident_id, created_at DESC);
