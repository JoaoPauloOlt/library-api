-- Manual production migration: introduce an explicit reservation state for physical copies.
-- Safe to execute more than once. Back up the database and record execution before deployment.
-- This script is transactional and records itself only after all changes succeed.

BEGIN;

-- Records successful, manually executed production schema upgrades.
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

ALTER TABLE book_copies
    DROP CONSTRAINT IF EXISTS chk_book_copy_status;

ALTER TABLE book_copies
    ADD CONSTRAINT chk_book_copy_status
    CHECK (status IN ('AVAILABLE', 'RESERVED', 'LOANED', 'MAINTENANCE', 'LOST', 'DISCARDED'));

-- Reconcile legacy requests that previously left their physical copy marked as AVAILABLE.
-- If an active/late loan also exists for the copy, its state takes precedence.
UPDATE book_copies AS copy
SET status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM loans loan
        WHERE loan.book_copy_id = copy.id
          AND loan.status IN ('ACTIVE', 'LATE')
    ) THEN 'LOANED'
    ELSE 'RESERVED'
END
WHERE copy.status IN ('AVAILABLE', 'RESERVED', 'LOANED')
  AND EXISTS (
      SELECT 1
      FROM loans loan
      WHERE loan.book_copy_id = copy.id
        AND loan.status IN ('REQUESTED', 'ACTIVE', 'LATE')
  );

INSERT INTO schema_migrations (version, description)
VALUES (
    '2026-11-13-reserve-book-copies',
    'Add RESERVED physical-copy status and reconcile open loan states'
)
ON CONFLICT (version) DO NOTHING;

COMMIT;
