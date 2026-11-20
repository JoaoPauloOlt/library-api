-- Manual production migration: enforce one open loan per physical copy.
-- Back up the database and record execution before deployment.
-- This migration is intentionally fail-fast: it does not rewrite or delete legacy loans.
-- The registry row is committed atomically with the partial unique index.

BEGIN;

-- Records successful, manually executed production schema upgrades.
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(120) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(120) NOT NULL DEFAULT CURRENT_USER
);

DO $$
DECLARE
    conflicting_copy_ids TEXT;
BEGIN
    SELECT string_agg(book_copy_id::TEXT, ', ' ORDER BY book_copy_id)
    INTO conflicting_copy_ids
    FROM (
        SELECT book_copy_id
        FROM loans
        WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE')
        GROUP BY book_copy_id
        HAVING COUNT(*) > 1
    ) AS conflicts;

    IF conflicting_copy_ids IS NOT NULL THEN
        RAISE EXCEPTION
            'Cannot enforce one open loan per copy. Resolve duplicate open loans for book_copy_id(s): %',
            conflicting_copy_ids;
    END IF;
END
$$;

DO $
DECLARE
    existing_index_oid OID;
    existing_index_valid BOOLEAN;
    existing_predicate TEXT;
BEGIN
    SELECT
        index_class.oid,
        index_info.indisunique
            AND index_info.indisvalid
            AND index_info.indnkeyatts = 1
            AND table_class.relname = 'loans'
            AND pg_get_indexdef(index_info.indexrelid, 1, TRUE) = 'book_copy_id',
        pg_get_expr(index_info.indpred, index_info.indrelid)
    INTO existing_index_oid, existing_index_valid, existing_predicate
    FROM pg_class index_class
    JOIN pg_namespace index_namespace ON index_namespace.oid = index_class.relnamespace
    JOIN pg_index index_info ON index_info.indexrelid = index_class.oid
    JOIN pg_class table_class ON table_class.oid = index_info.indrelid
    WHERE index_namespace.nspname = current_schema()
      AND index_class.relname = 'uq_loans_one_open_per_copy';

    IF existing_index_oid IS NULL THEN
        CREATE UNIQUE INDEX uq_loans_one_open_per_copy
            ON loans(book_copy_id)
            WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE');
    ELSIF NOT existing_index_valid
        OR existing_predicate IS NULL
        OR existing_predicate NOT LIKE '%REQUESTED%'
        OR existing_predicate NOT LIKE '%ACTIVE%'
        OR existing_predicate NOT LIKE '%LATE%'
        OR (length(existing_predicate) - length(replace(existing_predicate, '''', ''))) <> 6
    THEN
        RAISE EXCEPTION
            'Index uq_loans_one_open_per_copy already exists with an unexpected definition. Inspect pg_get_indexdef before proceeding.';
    END IF;
END
$;

INSERT INTO schema_migrations (version, description)
VALUES (
    '2026-11-20-one-open-loan-per-copy',
    'Enforce one open loan per physical copy'
)
ON CONFLICT (version) DO NOTHING;

COMMIT;
