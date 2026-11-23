-- Manual production migration: add indexes for dashboard aggregate queries.
-- Safe to execute more than once. Back up the database before deployment.

BEGIN;

CREATE INDEX IF NOT EXISTS idx_loans_user_request_date
    ON loans(user_id, request_date);

CREATE INDEX IF NOT EXISTS idx_loans_user_status_due_date
    ON loans(user_id, status, due_date);

INSERT INTO schema_migrations (version, description)
VALUES (
    '2026-11-23-dashboard-query-indexes',
    'Add indexes for user-scoped dashboard loan aggregates'
)
ON CONFLICT (version) DO NOTHING;

COMMIT;
