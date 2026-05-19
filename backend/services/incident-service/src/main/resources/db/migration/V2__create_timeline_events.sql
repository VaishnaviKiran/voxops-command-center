CREATE TABLE timeline_events (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    summary TEXT NOT NULL,
    source VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_timeline_events_incident_created_at
    ON timeline_events (incident_id, created_at DESC);

INSERT INTO timeline_events (id, incident_id, event_type, summary, source, created_at)
SELECT
    '4d8f92d8-5373-49bd-aa20-75d7f192ed34',
    id,
    'NOTE',
    'Incident room opened and initial investigation started.',
    'system',
    NOW()
FROM incidents
WHERE title = 'Checkout latency spike'
LIMIT 1;
