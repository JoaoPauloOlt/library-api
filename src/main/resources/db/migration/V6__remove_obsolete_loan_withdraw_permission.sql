DELETE FROM group_permissions
WHERE permission_id = (
    SELECT id
    FROM permissions
    WHERE name = 'LOAN_WITHDRAW'
);

DELETE FROM permissions
WHERE name = 'LOAN_WITHDRAW';
