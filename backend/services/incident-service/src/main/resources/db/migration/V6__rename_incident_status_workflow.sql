UPDATE incidents
SET status = 'OPEN'
WHERE status = 'ACTIVE';

UPDATE incidents
SET status = 'MITIGATING'
WHERE status = 'MITIGATED';
