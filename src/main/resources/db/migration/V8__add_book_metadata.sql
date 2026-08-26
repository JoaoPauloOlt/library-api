ALTER TABLE books
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN cover_url VARCHAR(500);

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9780451524935&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE isbn = '9780451524935';

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9780141439518&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE isbn = '9780141439518';

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9780316769488&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE isbn = '9780316769488';

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9780743273565&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE isbn = '9780743273565';

UPDATE books
SET cover_url = 'https://books.google.com/books/content?vid=isbn9781638494195&printsec=frontcover&img=1&zoom=1&source=gbs_api'
WHERE LOWER(title) = 'shadow slave';
