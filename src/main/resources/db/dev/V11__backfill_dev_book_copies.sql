-- DEV ONLY: ensure every existing development book has physical copies.
-- New books can define their copy quantity through BookInput.
-- This migration only fills books that currently have no registered copies.

INSERT INTO book_copies (book_id, barcode, status, location, active)
SELECT
    b.id,
    'BK-DEV-' || b.id || '-00' || copy_number,
    'AVAILABLE',
    'DEV-ACERVO',
    TRUE
FROM books b
CROSS JOIN generate_series(1, 3) AS copy_number
WHERE NOT EXISTS (
    SELECT 1
    FROM book_copies existing
    WHERE existing.book_id = b.id
)
ON CONFLICT (barcode) DO NOTHING;
