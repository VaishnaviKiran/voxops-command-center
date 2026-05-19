INSERT INTO incidents (id, title, severity, status, started_at)
VALUES (
    '65b71bcc-0732-4c7b-ac17-f42dc52e4eb9',
    'Checkout latency spike',
    'SEV2',
    'OPEN',
    CURRENT_TIMESTAMP
);

INSERT INTO runbook_documents (id, slug, title, service_name, tags, content, created_at)
VALUES (
    '11111111-1111-4111-8111-111111111111',
    'checkout-latency',
    'Checkout Latency and Error Rate Runbook',
    'checkout-api',
    'checkout,latency,error-rate',
  'Symptoms: checkout latency rises after deployment. Mitigation: rollback and monitor error rate.',
    CURRENT_TIMESTAMP
);
