CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_incidents_started_at ON incidents (started_at DESC);
CREATE INDEX idx_incidents_status ON incidents (status);

INSERT INTO incidents (id, title, severity, status, started_at)
VALUES (
    '65b71bcc-0732-4c7b-ac17-f42dc52e4eb9',
    'Checkout latency spike',
    'SEV2',
    'ACTIVE',
    NOW()
);
