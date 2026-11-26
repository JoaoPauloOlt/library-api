-- Normalize stored emails and enforce uniqueness regardless of case or surrounding whitespace.
-- Backup first. Updating email or creating the functional unique index fails safely if
-- duplicate addresses exist after normalization; resolve them before retrying.
-- Production deployment: run manually before deploying this invariant; fresh DB provisioning
-- includes this script after schema.sql. Never run it against a live DB without a backup.
BEGIN;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

-- Plain SQL statements are intentional: Spring's SQL initializer does not parse
-- PostgreSQL DO $$ ... $$ blocks reliably.
UPDATE users
SET email = lower(btrim(email))
WHERE email IS DISTINCT FROM lower(btrim(email));

-- Recreate this named index so an existing index with a stale definition cannot
-- silently bypass normalized-email uniqueness.
DROP INDEX IF EXISTS uq_users_email_normalized;
CREATE UNIQUE INDEX uq_users_email_normalized
    ON users (lower(btrim(email)));

INSERT INTO schema_migrations (version, description)
VALUES ('2026-11-26-normalize-user-email',
        'Normalize user email addresses and enforce case-insensitive uniqueness')
ON CONFLICT (version) DO NOTHING;

COMMIT;
