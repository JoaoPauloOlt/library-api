-- Manual production migration: introduce an explicit reservation state for physical copies.
-- Safe to execute more than once. Back up the database and record execution before deployment.

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
