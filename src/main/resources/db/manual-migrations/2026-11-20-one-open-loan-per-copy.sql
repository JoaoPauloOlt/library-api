-- Manual production migration: enforce one open loan per physical copy.
-- Back up the database and record execution before deployment.
-- This migration is intentionally fail-fast: it does not rewrite or delete legacy loans.
-- The registry row is committed atomically with the partial unique index.

BEGIN;

-- Records successful, manually executed production schema upgrades.
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

DO $$
DECLARE
    conflicting_copy_ids TEXT;
BEGIN
    SELECT string_agg(book_copy_id::TEXT, ', ' ORDER BY book_copy_id)
    INTO conflicting_copy_ids
    FROM (
        SELECT book_copy_id
        FROM loans
        WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE')
        GROUP BY book_copy_id
        HAVING COUNT(*) > 1
    ) AS conflicts;

    IF conflicting_copy_ids IS NOT NULL THEN
        RAISE EXCEPTION
            'Cannot enforce one open loan per copy. Resolve duplicate open loans for book_copy_id(s): %',
            conflicting_copy_ids;
    END IF;
END
$$;

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
