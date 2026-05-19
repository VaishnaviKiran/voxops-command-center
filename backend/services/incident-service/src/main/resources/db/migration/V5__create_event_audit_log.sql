CREATE TABLE IF NOT EXISTS event_audit_log (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    topic VARCHAR(160) NOT NULL,
    message_key VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    kafka_partition INTEGER,
    kafka_offset BIGINT,
    error_message TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_event_audit_log_aggregate_created_at
    ON event_audit_log (aggregate_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_event_audit_log_event_type
    ON event_audit_log (event_type);

CREATE INDEX IF NOT EXISTS idx_event_audit_log_status
    ON event_audit_log (status);
