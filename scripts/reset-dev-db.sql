-- DEV ONLY: clear application data and restore deterministic development fixtures.
-- Keep this list aligned with the application tables in src/main/resources/schema.sql.
-- The schema itself is preserved; only table rows and generated identities are reset.

TRUNCATE TABLE
    refresh_tokens,
    loans,
    book_copies,
    book_authors,
    authors,
    books,
    user_groups,
    users,
    group_permissions,
    permissions,
    groups
RESTART IDENTITY CASCADE;
