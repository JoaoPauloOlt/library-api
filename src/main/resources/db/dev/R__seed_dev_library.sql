-- DEV ONLY: deterministic demo data for local development and CI.
-- This migration is loaded only by the dev Flyway location.

-- Keep the existing dev admin account usable with documented credentials.
UPDATE users
SET password = '$2a$12$3HupHuNgkZwR9R/T0oP4mu/XT1yEDlc62BUzMnhshKYKXzCKE1ypS',
    status = 'ACTIVE',
    token_version = 0
WHERE email = 'admin@library.com';

INSERT INTO users (name, email, password, telephone, status)
VALUES
    ('Librarian', 'librarian@library.com', '$2a$12$J9Hz48x//9Ha2ZgxfIFTfezMhYQjkhR6JzOKQ1OmmwFiNOCyQaljS', '11999999998', 'ACTIVE'),
    ('User', 'user@library.com', '$2a$12$kvxospXmZPhv7VrbkTUVHeIyHZF3TYru3McaoCPsV2Z3RAY2rkczS', '11999999997', 'ACTIVE')
ON CONFLICT (email) DO UPDATE
SET password = EXCLUDED.password,
    status = EXCLUDED.status,
    token_version = 0;

INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
JOIN groups g ON g.name = 'ADMIN'
WHERE u.email = 'admin@library.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
JOIN groups g ON g.name = 'LIBRARIAN'
WHERE u.email = 'librarian@library.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
JOIN groups g ON g.name = 'USER'
WHERE u.email = 'user@library.com'
ON CONFLICT DO NOTHING;

-- Real authors/books; ISBNs below refer to specific published editions.
INSERT INTO authors (name, nationality)
VALUES
    ('George Orwell', 'British'),
    ('Jane Austen', 'British'),
    ('F. Scott Fitzgerald', 'American'),
    ('J. D. Salinger', 'American')
ON CONFLICT DO NOTHING;

INSERT INTO books (isbn, title, genre)
VALUES
    ('9780451524935', '1984', 'SCIENCE_FICTION'),
    ('9780141439518', 'Pride and Prejudice', 'CLASSIC'),
    ('9780743273565', 'The Great Gatsby', 'CLASSIC'),
    ('9780316769488', 'The Catcher in the Rye', 'DRAMA')
ON CONFLICT (isbn) DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b
JOIN authors a ON a.name = 'George Orwell'
WHERE b.isbn = '9780451524935'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b
JOIN authors a ON a.name = 'Jane Austen'
WHERE b.isbn = '9780141439518'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b
JOIN authors a ON a.name = 'F. Scott Fitzgerald'
WHERE b.isbn = '9780743273565'
ON CONFLICT DO NOTHING;

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id
FROM books b
JOIN authors a ON a.name = 'J. D. Salinger'
WHERE b.isbn = '9780316769488'
ON CONFLICT DO NOTHING;

INSERT INTO book_copies (book_id, barcode, status, location, active)
SELECT b.id, data.barcode, data.status, data.location, TRUE
FROM books b
JOIN (
    VALUES
        ('9780451524935', 'BC-1984-001', 'LOANED', 'A-01'),
        ('9780451524935', 'BC-1984-002', 'AVAILABLE', 'A-01'),
        ('9780141439518', 'BC-PP-001', 'AVAILABLE', 'A-02'),
        ('9780141439518', 'BC-PP-002', 'AVAILABLE', 'A-02'),
        ('9780743273565', 'BC-GATSBY-001', 'AVAILABLE', 'B-01'),
        ('9780743273565', 'BC-GATSBY-002', 'AVAILABLE', 'B-01'),
        ('9780316769488', 'BC-CATCHER-001', 'MAINTENANCE', 'B-02'),
        ('9780316769488', 'BC-CATCHER-002', 'AVAILABLE', 'B-02')
) AS data(isbn, barcode, status, location)
ON b.isbn = data.isbn
ON CONFLICT (barcode) DO NOTHING;

-- Active loan for the USER account.
INSERT INTO loans (
    user_id, book_copy_id, status, request_date,
    approval_date, withdrawable_date, due_date, return_date
)
SELECT
    u.id,
    bc.id,
    'ACTIVE',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    NULL
FROM users u
JOIN book_copies bc ON bc.barcode = 'BC-1984-001'
WHERE u.email = 'user@library.com'
  AND NOT EXISTS (
      SELECT 1 FROM loans l
      WHERE l.user_id = u.id AND l.book_copy_id = bc.id AND l.status = 'ACTIVE'
  );

-- Requested loan for the USER account; the copy remains available until approval.
INSERT INTO loans (
    user_id, book_copy_id, status, request_date,
    approval_date, withdrawable_date, due_date, return_date
)
SELECT
    u.id,
    bc.id,
    'REQUESTED',
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    NULL,
    NULL,
    NULL,
    NULL
FROM users u
JOIN book_copies bc ON bc.barcode = 'BC-PP-001'
WHERE u.email = 'user@library.com'
  AND NOT EXISTS (
      SELECT 1 FROM loans l
      WHERE l.user_id = u.id AND l.book_copy_id = bc.id AND l.status = 'REQUESTED'
  );

-- Returned loan to populate the general history view.
INSERT INTO loans (
    user_id, book_copy_id, status, request_date,
    approval_date, withdrawable_date, due_date, return_date
)
SELECT
    u.id,
    bc.id,
    'RETURNED',
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP - INTERVAL '29 days',
    CURRENT_TIMESTAMP - INTERVAL '28 days',
    CURRENT_TIMESTAMP - INTERVAL '15 days',
    CURRENT_TIMESTAMP - INTERVAL '14 days'
FROM users u
JOIN book_copies bc ON bc.barcode = 'BC-GATSBY-001'
WHERE u.email = 'user@library.com'
  AND NOT EXISTS (
      SELECT 1 FROM loans l
      WHERE l.user_id = u.id AND l.book_copy_id = bc.id AND l.status = 'RETURNED'
  );
