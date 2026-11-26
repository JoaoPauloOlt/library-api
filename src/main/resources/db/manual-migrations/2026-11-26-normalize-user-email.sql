-- Normalize stored emails and enforce uniqueness regardless of case or surrounding whitespace.
-- Backup first. This aborts if existing records collide after normalization.
BEGIN;
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
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized
    ON users (lower(btrim(email)));
INSERT INTO schema_migrations (version, description)
VALUES ('2026-11-26-normalize-user-email',
        'Normalize user email addresses and enforce case-insensitive uniqueness')
ON CONFLICT (version) DO NOTHING;
COMMIT;
