ALTER TABLE books
    ADD COLUMN publisher VARCHAR(255),
    ADD COLUMN publication_date DATE,
    ADD COLUMN page_count INTEGER,
    ADD COLUMN language VARCHAR(100),
    ADD COLUMN description TEXT,
    ADD COLUMN cover_url VARCHAR(500);
