INSERT INTO permissions (name)
SELECT 'CREATE_BOOK'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CREATE_BOOK');

INSERT INTO permissions (name)
SELECT 'DELETE_BOOK'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'DELETE_BOOK');

INSERT INTO permissions (name)
SELECT 'VIEW_BOOK'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_BOOK');

INSERT INTO permissions (name)
SELECT 'CREATE_AUTHOR'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CREATE_AUTHOR');

INSERT INTO permissions (name)
SELECT 'DELETE_AUTHOR'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'DELETE_AUTHOR');

INSERT INTO permissions (name)
SELECT 'ADMIN'
    WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'ADMIN');


INSERT INTO groups (name, description)
SELECT 'ADMIN', 'Full access'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'ADMIN');

INSERT INTO groups (name, description)
SELECT 'LIBRARIAN', 'Manage books'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'LIBRARIAN');

INSERT INTO groups (name, description)
SELECT 'USER', 'Basic access'
    WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'USER');



INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
         JOIN permissions p ON p.name IN (
                                          'CREATE_BOOK','DELETE_BOOK','CREATE_AUTHOR','DELETE_AUTHOR','VIEW_BOOK'
    )
WHERE g.name = 'LIBRARIAN'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g
         JOIN permissions p ON p.name = 'VIEW_BOOK'
WHERE g.name = 'USER'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);



INSERT INTO users (name, email, password, telephone, status, date_register)
SELECT
    'Admin',
    'admin@library.com',
    '$2a$12$CGXlodrxmxPg1mVNI/a3S./vYEnXtuzrJIWul9bNjvsgcPXaIYoka',
    '11999999999',
    'ACTIVE',
    CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@library.com'
);


INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u, groups g
WHERE u.email = 'admin@library.com'
  AND g.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_groups ug
    WHERE ug.user_id = u.id AND ug.group_id = g.id
);