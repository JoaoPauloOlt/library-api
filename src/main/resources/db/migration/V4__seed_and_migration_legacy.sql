INSERT INTO permissions (name) VALUES
    ('CREATE_BOOK'),
    ('DELETE_BOOK'),
    ('VIEW_BOOK'),
    ('CREATE_AUTHOR'),
    ('DELETE_AUTHOR');

INSERT INTO groups (name, description) VALUES
    ('ADMIN', 'Full access'),
    ('LIBRARIAN', 'Manage books'),
    ('USER', 'Basic access');

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
CROSS JOIN permissions p
WHERE g.name = 'ADMIN';

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p ON p.name IN (
    'CREATE_BOOK',
    'DELETE_BOOK',
    'VIEW_BOOK',
    'CREATE_AUTHOR',
    'DELETE_AUTHOR'
)
WHERE g.name = 'LIBRARIAN';

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
JOIN permissions p ON p.name = 'VIEW_BOOK'
WHERE g.name = 'USER';

INSERT INTO users (name, email, password, telephone, status)
VALUES (
    'Admin',
    'admin@library.com',
    '$2a$12$CGXlodrxmxPg1mVNI/a3S./vYEnXtuzrJIWul9bNjvsgcPXaIYoka',
    '11999999999',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
JOIN groups g ON g.name = 'ADMIN'
WHERE u.email = 'admin@library.com';

ALTER TABLE users
ADD CONSTRAINT chk_users_status
CHECK (status IN ('ACTIVE','INACTIVE'));

ALTER TABLE book_copies
ADD CONSTRAINT chk_copy_status
CHECK (status IN ('AVAILABLE','LOANED','MAINTENANCE'));

ALTER TABLE loans
ADD CONSTRAINT chk_loan_status
CHECK (status IN (
    'REQUESTED','APPROVED','ACTIVE','RETURNED','LATE','CANCELED'
));