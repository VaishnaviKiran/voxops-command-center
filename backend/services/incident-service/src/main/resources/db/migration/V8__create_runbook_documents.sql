CREATE TABLE runbook_documents (
    id UUID PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    tags TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_runbook_documents_service_name ON runbook_documents (service_name);

INSERT INTO runbook_documents (id, slug, title, service_name, tags, content, created_at)
VALUES
(
    '11111111-1111-4111-8111-111111111111',
    'checkout-latency',
    'Checkout Latency and Error Rate Runbook',
    'checkout-api',
    'checkout,latency,error-rate,payment,rollback',
    'Symptoms: checkout p95 latency rises, payment authorization times out, or HTTP 5xx rate increases. Triage: compare deploy timeline, checkout-api p95 latency, database connection pool saturation, payment provider latency, and cache hit rate. Mitigation: pause rollout, rollback latest checkout-api deployment, shift traffic away from unhealthy region, and disable non-critical promotions if dependency latency is high. Validate: p95 latency below SLO for 15 minutes, error rate below 1%, successful synthetic checkout, and payment authorization success rate recovered. Escalate to payments on-call if provider latency remains high after rollback.',
    NOW()
),
(
    '22222222-2222-4222-8222-222222222222',
    'search-api-degradation',
    'Search API Degradation Runbook',
    'search-api',
    'search,latency,index,cache,elasticsearch',
    'Symptoms: search requests timeout, result quality drops, or index freshness lags. Triage: inspect search-api p95 latency, Elasticsearch cluster health, query error logs, cache hit rate, and recent indexer deployments. Mitigation: increase cache TTL, route traffic to warm replicas, disable expensive ranking features, or rollback query parser changes. Validate: query success rate recovers, p95 latency returns below SLO, and index lag is under 5 minutes.',
    NOW()
),
(
    '33333333-3333-4333-8333-333333333333',
    'database-connection-saturation',
    'Database Connection Saturation Runbook',
    'postgres',
    'database,postgres,connection-pool,latency,saturation',
    'Symptoms: application latency increases, connection acquisition times out, or database CPU spikes. Triage: check active connections, blocked queries, slow query logs, connection pool usage, and recent migrations. Mitigation: scale read replicas, reduce pool max size for noisy services, kill runaway queries after approval, pause background jobs, or rollback risky migrations. Validate: blocked queries cleared, pool utilization below 80%, and service latency recovered.',
    NOW()
),
(
    '44444444-4444-4444-8444-444444444444',
    'kafka-publish-failures',
    'Kafka Publish Failure Runbook',
    'kafka',
    'kafka,event,audit,publish,broker',
    'Symptoms: domain events remain pending or failed, producer retries increase, or broker connection errors appear. Triage: inspect broker health, topic existence, producer bootstrap servers, serialization errors, and event_audit_log failure messages. Mitigation: restart unhealthy broker, recreate missing topic, fix producer configuration, or temporarily disable non-critical event publishing. Validate: new events reach PUBLISHED status with partition and offset.',
    NOW()
),
(
    '55555555-5555-4555-8555-555555555555',
    'voice-transcript-quality',
    'Voice Transcript Quality Runbook',
    'voice-stream-service',
    'voice,transcript,stt,microphone,websocket',
    'Symptoms: transcript text is missing, inaccurate, duplicated, or delayed. Triage: confirm browser microphone permission, WebSocket connectivity, speech recognition support, background noise, confidence scores, and transcript API authorization. Mitigation: ask responder to speak clearly, reduce background noise, restart voice stream, use Chrome, and retry transcript capture. Validate: transcript segments are saved with acceptable confidence and timeline events are created.',
    NOW()
);
