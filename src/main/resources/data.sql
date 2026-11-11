-- Shared RBAC seed. Safe to execute on every application startup.
-- Demo accounts and catalog fixtures belong exclusively in data-dev.sql.

INSERT INTO permissions (name)
VALUES
    ('BOOK_READ'),
    ('BOOK_CREATE'),
    ('BOOK_UPDATE'),
    ('BOOK_DELETE'),
    ('AUTHOR_READ'),
    ('AUTHOR_CREATE'),
    ('AUTHOR_UPDATE'),
    ('AUTHOR_DELETE'),
    ('BOOK_COPY_READ'),
    ('BOOK_COPY_CREATE'),
    ('BOOK_COPY_UPDATE'),
    ('BOOK_COPY_DELETE'),
    ('LOAN_CREATE'),
    ('LOAN_APPROVE'),
    ('LOAN_CANCEL'),
    ('LOAN_RETURN'),
    ('LOAN_READ_ALL'),
    ('USER_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO groups (name, description)
VALUES
    ('ADMIN', 'Full access'),
    ('LIBRARIAN', 'Library management'),
    ('USER', 'Basic user')
ON CONFLICT (name) DO NOTHING;

-- Remove the permission retired by the former V7 migration.
DELETE FROM group_permissions
WHERE permission_id IN (
    SELECT id
    FROM permissions
    WHERE name = 'LOAN_WITHDRAW'
);

DELETE FROM permissions
WHERE name = 'LOAN_WITHDRAW';

-- ADMIN receives every permission currently defined by the application.
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
CROSS JOIN permissions p
WHERE g.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- LIBRARIAN can manage the catalog, copies and circulation.
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p
    ON p.name IN (
        'BOOK_READ',
        'BOOK_CREATE',
        'BOOK_UPDATE',
        'BOOK_DELETE',
        'AUTHOR_READ',
        'AUTHOR_CREATE',
        'AUTHOR_UPDATE',
        'AUTHOR_DELETE',
        'BOOK_COPY_READ',
        'BOOK_COPY_CREATE',
        'BOOK_COPY_UPDATE',
        'BOOK_COPY_DELETE',
        'LOAN_CREATE',
        'LOAN_APPROVE',
        'LOAN_CANCEL',
        'LOAN_RETURN',
        'LOAN_READ_ALL'
    )
WHERE g.name = 'LIBRARIAN'
ON CONFLICT DO NOTHING;

-- USER can read the catalog and manage their own loan requests.
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p
    ON p.name IN (
        'BOOK_READ',
        'AUTHOR_READ',
        'LOAN_CREATE',
        'LOAN_CANCEL'
    )
WHERE g.name = 'USER'
ON CONFLICT DO NOTHING;
