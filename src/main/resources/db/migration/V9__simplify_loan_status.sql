-- APPROVED is no longer a distinct domain state.
-- Existing approved loans are promoted to ACTIVE and their copies become LOANED.

UPDATE book_copies bc
SET status = 'LOANED'
FROM loans l
WHERE l.book_copy_id = bc.id
  AND l.status = 'APPROVED'
  AND bc.status = 'AVAILABLE';

UPDATE loans
SET status = 'ACTIVE',
    withdrawable_date = COALESCE(withdrawable_date, approval_date, request_date),
    due_date = COALESCE(
        due_date,
        COALESCE(approval_date, request_date) + INTERVAL '7 days'
    )
WHERE status = 'APPROVED';

ALTER TABLE loans
    DROP CONSTRAINT chk_loan_status;

ALTER TABLE loans
    ADD CONSTRAINT chk_loan_status
    CHECK (
        status IN (
            'REQUESTED',
            'ACTIVE',
            'RETURNED',
            'LATE',
            'CANCELED'
        )
    );
