-- Development-only equivalent of the production one-open-loan-per-copy migration.
-- Kept free of PL/pgSQL blocks so Spring SQL initialization can execute it safely.
-- The unique index creation fails if legacy data contains conflicting open loans.

BEGIN;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loans_one_open_per_copy
    ON loans(book_copy_id)
    WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE');

INSERT INTO schema_migrations (version, description)
VALUES (
    '2026-11-20-one-open-loan-per-copy',
    'Enforce one open loan per physical copy'
)
ON CONFLICT (version) DO NOTHING;

COMMIT;
