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

    ('LOAN_CREATE'),
    ('LOAN_APPROVE'),
    ('LOAN_CANCEL'),
    ('LOAN_WITHDRAW'),
    ('LOAN_RETURN'),
    ('LOAN_MANAGE'),

    ('USER_ADMIN');

INSERT INTO groups (name, description)
VALUES
    ('ADMIN', 'Full access'),
    ('LIBRARIAN', 'Library management'),
    ('USER', 'Basic user');

-- ADMIN
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
CROSS JOIN permissions p
WHERE g.name = 'ADMIN';

-- LIBRARIAN
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

                            'LOAN_CREATE',
                            'LOAN_APPROVE',
                            'LOAN_CANCEL',
                            'LOAN_WITHDRAW',
                            'LOAN_RETURN',
                            'LOAN_MANAGE'
                  )
WHERE g.name = 'LIBRARIAN';

-- USER
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
WHERE g.name = 'USER';

INSERT INTO users (
    name,
    email,
    password,
    telephone,
    status
)
VALUES (
           'Admin',
           'admin@library.com',
           '$2a$12$CGXlodrxmxPg1mVNI/a3S./vYEnXtuzrJIWul9bNjvsgcPXaIYoka',
           '11999999999',
           'ACTIVE'
       );

INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
JOIN groups g
ON g.name = 'ADMIN'
WHERE u.email = 'admin@library.com';