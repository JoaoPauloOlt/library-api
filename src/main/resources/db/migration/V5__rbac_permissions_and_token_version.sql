-- Token invalidation support (bump on password change / global revoke)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

-- Migrate legacy permission names to canonical RBAC identifiers
UPDATE permissions SET name = 'BOOK_CREATE' WHERE name = 'CREATE_BOOK';
UPDATE permissions SET name = 'BOOK_DELETE' WHERE name = 'DELETE_BOOK';
UPDATE permissions SET name = 'BOOK_READ'    WHERE name = 'VIEW_BOOK';
UPDATE permissions SET name = 'AUTHOR_CREATE' WHERE name = 'CREATE_AUTHOR';
UPDATE permissions SET name = 'AUTHOR_DELETE' WHERE name = 'DELETE_AUTHOR';

INSERT INTO permissions (name) VALUES
    ('BOOK_UPDATE'),
    ('AUTHOR_READ'),
    ('AUTHOR_UPDATE'),
    ('CREATE_LOAN'),
    ('APPROVE_LOAN'),
    ('WITHDRAW_LOAN'),
    ('RETURN_BOOK'),
    ('CANCEL_LOAN'),
    ('USER_ADMIN'),
    ('LOAN_MANAGE')
ON CONFLICT (name) DO NOTHING;

-- ADMIN: all permissions
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
CROSS JOIN permissions p
WHERE g.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- LIBRARIAN: catalog + loan operations
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p ON p.name IN (
    'BOOK_READ', 'BOOK_CREATE', 'BOOK_UPDATE', 'BOOK_DELETE',
    'AUTHOR_READ', 'AUTHOR_CREATE', 'AUTHOR_UPDATE', 'AUTHOR_DELETE',
    'LOAN_MANAGE', 'APPROVE_LOAN', 'WITHDRAW_LOAN', 'RETURN_BOOK', 'CANCEL_LOAN'
)
WHERE g.name = 'LIBRARIAN'
ON CONFLICT DO NOTHING;

-- USER: read catalog + self-service loans
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p ON p.name IN (
    'BOOK_READ', 'AUTHOR_READ', 'CREATE_LOAN', 'CANCEL_LOAN'
)
WHERE g.name = 'USER'
ON CONFLICT DO NOTHING;
