-- Manual production migration: enforce one open loan per physical copy.
-- Back up the database and record execution before deployment.
-- This migration is intentionally fail-fast: it does not rewrite or delete legacy loans.

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
