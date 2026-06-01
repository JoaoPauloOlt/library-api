CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    telephone VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL,
    token_version INTEGER NOT NULL DEFAULT 0,

    date_register TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_status
                   CHECK ( status in ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE group_permissions (
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (group_id, permission_id),

    CONSTRAINT fk_group_permissions_group
        FOREIGN KEY (group_id)
            REFERENCES groups(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_permissions_permission
        FOREIGN KEY (permission_id)
            REFERENCES permissions(id)
            ON DELETE CASCADE
);

CREATE TABLE user_groups (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, group_id),

    CONSTRAINT fk_user_groups_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_groups_group
        FOREIGN KEY (group_id)
            REFERENCES groups(id)
            ON DELETE CASCADE
);