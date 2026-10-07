CREATE TABLE revoked_access_tokens (
    jti VARCHAR(36) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_revoked_access_tokens_expires_at
    ON revoked_access_tokens (expires_at);
