-- Initial PostgreSQL schema for a new database.
-- Idempotent by design: existing tables and their data are never dropped or truncated.
-- Future production schema changes must be handled by reviewed, manual SQL scripts.

-- Records successful, manually executed production schema upgrades.
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    telephone VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    token_version INTEGER NOT NULL DEFAULT 0,
    date_register TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS group_permissions (
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, permission_id),
    CONSTRAINT fk_group_permissions_group
        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_groups (
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, group_id),
    CONSTRAINT fk_user_groups_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_groups_group
        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);



CREATE TABLE IF NOT EXISTS authors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    nationality VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(13) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cover_url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_book_authors_book
        FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_book_authors_author
        FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_book_copy_status
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'LOANED', 'MAINTENANCE', 'LOST', 'DISCARDED')),
    CONSTRAINT fk_book_copy_book
        FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_book_copies_book_id ON book_copies(book_id);

CREATE TABLE IF NOT EXISTS loans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_copy_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_date TIMESTAMP NOT NULL,
    approval_date TIMESTAMP,
    withdrawable_date TIMESTAMP,
    due_date TIMESTAMP,
    return_date TIMESTAMP,
    renewal_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_loan_status
        CHECK (status IN ('REQUESTED', 'ACTIVE', 'RETURNED', 'LATE', 'CANCELED')),
    CONSTRAINT fk_loans_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_book_copy
        FOREIGN KEY (book_copy_id) REFERENCES book_copies(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_book_copy_id ON loans(book_copy_id);
CREATE INDEX IF NOT EXISTS idx_loans_user_request_date ON loans(user_id, request_date);
CREATE INDEX IF NOT EXISTS idx_loans_user_status_due_date ON loans(user_id, status, due_date);

-- A physical copy can have only one open loan at a time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_loans_one_open_per_copy
    ON loans(book_copy_id)
    WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE');

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    jti VARCHAR(36) NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_jti VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS revoked_access_tokens (
    jti VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_access_tokens_expires_at
    ON revoked_access_tokens(expires_at);


CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_available
    ON outbox_events(status, available_at);

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON outbox_events(aggregate_type, aggregate_id);


CREATE TABLE IF NOT EXISTS fines (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    late_days INTEGER NOT NULL,
    daily_rate NUMERIC(12, 2) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    waived_at TIMESTAMPTZ,
    waived_by BIGINT,
    waiver_reason VARCHAR(500),
    CONSTRAINT chk_fines_late_days CHECK (late_days > 0),
    CONSTRAINT chk_fines_daily_rate CHECK (daily_rate > 0),
    CONSTRAINT chk_fines_amount CHECK (amount > 0),
    CONSTRAINT chk_fines_status CHECK (status IN ('OPEN', 'WAIVED')),
    CONSTRAINT fk_fines_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fines_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_fines_waived_by FOREIGN KEY (waived_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_fines_user_status ON fines(user_id, status);
