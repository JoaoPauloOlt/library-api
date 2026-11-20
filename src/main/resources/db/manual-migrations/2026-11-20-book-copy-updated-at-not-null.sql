-- Manual production migration: make book-copy update timestamps non-null.
-- Hibernate 7 maps @UpdateTimestamp as NOT NULL; preserve existing rows and
-- provide a default for inserts performed outside Hibernate.

BEGIN;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

UPDATE book_copies
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE book_copies
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE book_copies
    ALTER COLUMN updated_at SET NOT NULL;

INSERT INTO schema_migrations (version, description)
VALUES (
    '2026-11-20-book-copy-updated-at-not-null',
    'Align book-copy updated_at with Hibernate 7 non-null mapping'
)
ON CONFLICT (version) DO NOTHING;

COMMIT;
