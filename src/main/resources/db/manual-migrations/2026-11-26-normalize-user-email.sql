-- Normalize stored emails and enforce uniqueness regardless of case or surrounding whitespace.
-- Backup first. This aborts if existing records collide after normalization.
-- Production deployment: run manually before deploying this invariant; fresh DB provisioning
-- includes this script after schema.sql. Never run it against a live DB without a backup.
BEGIN;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM users
        GROUP BY lower(btrim(email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize users.email: duplicate addresses ignoring case/whitespace. Resolve duplicate accounts before retrying.';
    END IF;
END
$$;

UPDATE users
SET email = lower(btrim(email))
WHERE email IS DISTINCT FROM lower(btrim(email));

DO $$
DECLARE
    existing_index_oid OID;
    existing_index_valid BOOLEAN;
    existing_index_definition TEXT;
BEGIN
    SELECT index_class.oid,
           index_info.indisunique AND index_info.indisvalid,
           pg_get_indexdef(index_info.indexrelid)
    INTO existing_index_oid, existing_index_valid, existing_index_definition
    FROM pg_class index_class
    JOIN pg_namespace index_namespace ON index_namespace.oid = index_class.relnamespace
    JOIN pg_index index_info ON index_info.indexrelid = index_class.oid
    WHERE index_namespace.nspname = current_schema()
      AND index_class.relname = 'uq_users_email_normalized';

    IF existing_index_oid IS NULL THEN
        CREATE UNIQUE INDEX uq_users_email_normalized
            ON users (lower(btrim(email)));
    ELSIF NOT existing_index_valid
       OR existing_index_definition NOT LIKE '%lower(btrim(email))%' THEN
        RAISE EXCEPTION 'Index uq_users_email_normalized exists with an unexpected definition. Inspect pg_get_indexdef before proceeding.';
    END IF;
END
$$;

INSERT INTO schema_migrations (version, description)
VALUES ('2026-11-26-normalize-user-email',
        'Normalize user email addresses and enforce case-insensitive uniqueness')
ON CONFLICT (version) DO NOTHING;

COMMIT;
