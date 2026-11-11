-- DEV ONLY: deterministic demo data for local development and CI.
-- This script is loaded after data.sql and only with the dev profile.

-- Keep development accounts available on both empty and reused databases.
INSERT INTO users (
    name, email, password, telephone, status, token_version, date_register
)
VALUES
    ('Admin', 'admin@library.com', '$2a$12$3HupHuNgkZwR9R/T0oP4mu/XT1yEDlc62BUzMnhshKYKXzCKE1ypS', '11999999999', 'ACTIVE', 0, CURRENT_TIMESTAMP),
    ('Librarian', 'librarian@library.com', '$2a$12$J9Hz48x//9Ha2ZgxfIFTfezMhYQjkhR6JzOKQ1OmmwFiNOCyQaljS', '11999999998', 'ACTIVE', 0, CURRENT_TIMESTAMP),
    ('User', 'user@library.com', '$2a$12$kvxospXmZPhv7VrbkTUVHeIyHZF3TYru3McaoCPsV2Z3RAY2rkczS', '11999999997', 'ACTIVE', 0, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    password = EXCLUDED.password,
    telephone = EXCLUDED.telephone,
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

-- Insert authors only when they do not already exist.
INSERT INTO authors (name, nationality)
SELECT seed.name, seed.nationality
FROM (
    VALUES
        ('George Orwell', 'British'),
        ('Jane Austen', 'British'),
        ('F. Scott Fitzgerald', 'American'),
        ('J. D. Salinger', 'American')
) AS seed(name, nationality)
WHERE NOT EXISTS (
    SELECT 1
    FROM authors a
    WHERE LOWER(a.name) = LOWER(seed.name)
);

-- Demo books retain the Portuguese descriptions previously provided by V12.
INSERT INTO books (isbn, title, genre, description)
VALUES
    ('9780451524935', '1984', 'SCIENCE_FICTION', 'Em uma sociedade totalitária onde o Estado controla a informação, Winston Smith trabalha alterando registros históricos. Ao questionar a realidade imposta pelo Partido, ele começa a buscar uma verdade que pode lhe custar tudo.'),
    ('9780141439518', 'Pride and Prejudice', 'CLASSIC', 'Elizabeth Bennet enfrenta as expectativas sociais de sua época enquanto conhece o reservado e orgulhoso Mr. Darcy. A história explora relações, preconceitos, classe social e as consequências de julgar alguém pelas primeiras impressões.'),
    ('9780743273565', 'The Great Gatsby', 'CLASSIC', 'No verão de 1922, Nick Carraway observa a vida extravagante de seu vizinho Jay Gatsby e seu círculo social em Long Island. Entre festas, riqueza e segredos, Gatsby tenta recuperar um amor do passado.'),
    ('9780316769488', 'The Catcher in the Rye', 'DRAMA', 'Holden Caulfield narra alguns dias turbulentos após ser expulso de sua escola. Entre encontros em Nova York e reflexões sobre a vida adulta, ele procura sentido, autenticidade e um lugar onde se sinta pertencente.')
ON CONFLICT (isbn) DO UPDATE
SET title = EXCLUDED.title,
    genre = EXCLUDED.genre,
    description = EXCLUDED.description;

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
SELECT b.id, seed.barcode, seed.status, seed.location, TRUE
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
) AS seed(isbn, barcode, status, location)
    ON b.isbn = seed.isbn
ON CONFLICT (barcode) DO NOTHING;

-- Preserve the old V11 behavior for any development book without copies.
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

-- Preserve the cover URLs previously populated by V8.
UPDATE books
SET cover_url = CASE isbn
    WHEN '9780451524935' THEN 'https://books.google.com/books/content?vid=isbn9780451524935&printsec=frontcover&img=1&zoom=1&source=gbs_api'
    WHEN '9780141439518' THEN 'https://books.google.com/books/content?vid=isbn9780141439518&printsec=frontcover&img=1&zoom=1&source=gbs_api'
    WHEN '9780316769488' THEN 'https://books.google.com/books/content?vid=isbn9780316769488&printsec=frontcover&img=1&zoom=1&source=gbs_api'
    WHEN '9780743273565' THEN 'https://books.google.com/books/content?vid=isbn9780743273565&printsec=frontcover&img=1&zoom=1&source=gbs_api'
    ELSE cover_url
END
WHERE isbn IN (
    '9780451524935',
    '9780141439518',
    '9780316769488',
    '9780743273565'
);

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9781638494195&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE LOWER(title) = 'shadow slave';

-- Active loan scenario for the USER account.
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
      SELECT 1
      FROM loans l
      WHERE l.user_id = u.id
        AND l.book_copy_id = bc.id
  );

-- Requested loan scenario; the copy remains available until approval.
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
      SELECT 1
      FROM loans l
      WHERE l.user_id = u.id
        AND l.book_copy_id = bc.id
  );

-- Returned loan scenario for the general history view.
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
      SELECT 1
      FROM loans l
      WHERE l.user_id = u.id
        AND l.book_copy_id = bc.id
  );
