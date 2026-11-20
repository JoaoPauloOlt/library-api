# Database migration runbook

## Purpose and operating model

This repository does not use Flyway or another automatic migration runner. Production schema changes are reviewed SQL files and are executed manually by an operator.

- `schema.sql` is the current bootstrap schema for a **new, empty database**. It contains the current database structure; it is not a replayable history of every change.
- `db/manual-migrations/` contains ordered changes that must be applied to **existing databases** when moving from an older application version.
- `schema_migrations` records successful execution of manual production migrations. It does not replace backups, review, or validation.
- Production startup does not execute files from `db/manual-migrations/`. Hibernate runs with `ddl-auto=validate`; it must not create or update the production schema.

Never run a manual migration against production without a verified backup and a maintenance/deployment plan.

## Naming and authoring convention

Use one SQL file per schema change:

```text
src/main/resources/db/manual-migrations/YYYY-MM-DD-short-description.sql
```

Requirements:

1. Use the intended production execution date and a concise, stable description.
2. Keep scripts narrowly scoped and reviewable.
3. Prefer transactional DDL and data changes. Start with `BEGIN;` and finish with `COMMIT;` where the PostgreSQL operations support it.
4. Make scripts safe to re-run where practical, using guards such as `IF EXISTS`, `IF NOT EXISTS`, and conflict-safe registry inserts.
5. Detect and report incompatible legacy data before adding constraints or indexes. Do not silently delete or rewrite business records to make a migration pass.
6. Create or update the `schema_migrations` table in the script so the script can be run before a new application version has bootstrapped that table.
7. Insert the migration version into `schema_migrations` only after all migration operations succeed, in the same transaction.
8. Document prerequisites, verification queries, and rollback limitations in this runbook.

The migration version is the filename without `.sql`, for example `2026-11-20-one-open-loan-per-copy`.

## Migration registry

The registry has this structure:

| Column | Meaning |
|---|---|
| `version` | Primary key; migration filename without extension |
| `description` | Human-readable change summary |
| `installed_at` | Database timestamp when the migration completed |
| `installed_by` | PostgreSQL role that executed it |

Inspect recorded executions:

```sql
SELECT version, description, installed_at, installed_by
FROM schema_migrations
ORDER BY version;
```

A row must be inserted only after confirming that the corresponding migration completed successfully. Never insert a row merely to bypass a migration. If a migration was applied before the registry existed, verify the resulting schema and data first, then backfill its row explicitly as an audited administrative action.

For a newly created database, an empty registry is expected: `schema.sql` already represents the current schema. The registry tracks manual upgrades applied to existing databases; it is not a list of bootstrap DDL statements.

## Existing manual migrations

Execute applicable migrations in ascending filename order. Check the registry before each execution and do not re-run a migration that is already recorded.

### 1. `2026-11-13-reserve-book-copies.sql`

**Purpose:** Add `RESERVED` to the allowed physical-copy statuses and reconcile copies associated with open loan requests. An active/late loan takes precedence and marks the copy `LOANED`; otherwise an open request marks it `RESERVED`.

**Prerequisite:** Back up the database. Confirm the application version being deployed expects the reservation lifecycle.

**Execute:**

```bash
psql "$PGURI" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/manual-migrations/2026-11-13-reserve-book-copies.sql
```

**Verify:**

```sql
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'book_copies'::regclass
  AND conname = 'chk_book_copy_status';

SELECT status, COUNT(*)
FROM book_copies
GROUP BY status
ORDER BY status;

SELECT version, installed_at, installed_by
FROM schema_migrations
WHERE version = '2026-11-13-reserve-book-copies';
```

**Rollback:** There is no safe blind rollback. The migration may have changed copy states to `RESERVED` or `LOANED`, and the deployed application may rely on `RESERVED`. A rollback requires stopping the newer application, reconciling every affected copy against its loan history, proving that no `RESERVED` status remains, and only then restoring the previous check constraint in a separately reviewed transaction. Do not simply change `RESERVED` to `AVAILABLE`.

### 2. `2026-11-20-book-copy-updated-at-not-null.sql`

**Purpose:** Align the `book_copies.updated_at` column with Hibernate 7's non-null mapping and provide a database default for direct SQL inserts. Existing null values are backfilled from `created_at`; no copy or loan records are deleted.

**Prerequisite:** Back up the database and confirm `book_copies.created_at` is populated.

**Execute:**

```bash
psql "$PGURI" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/manual-migrations/2026-11-20-book-copy-updated-at-not-null.sql
```

**Verify:**

```sql
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name = 'book_copies'
  AND column_name = 'updated_at';

SELECT COUNT(*)
FROM book_copies
WHERE updated_at IS NULL;

SELECT version, installed_at, installed_by
FROM schema_migrations
WHERE version = '2026-11-20-book-copy-updated-at-not-null';
```

Expected: `is_nullable = 'NO'`, a `CURRENT_TIMESTAMP` default, and zero null rows. The migration is forward-compatible with the previous application version; no rollback is recommended because the upgraded Hibernate mapping requires the invariant.

### 3. `2026-11-20-one-open-loan-per-copy.sql`

**Purpose:** Enforce at database level that each physical copy has at most one open loan. Open statuses are `REQUESTED`, `ACTIVE` and `LATE`. Historical terminal statuses such as `RETURNED` and `CANCELED` remain unrestricted.

**Prerequisite:** Back up the database and resolve any duplicate open loans reported by the script. The script deliberately fails rather than choosing which business record to keep.

**Execute:**

```bash
psql "$PGURI" -v ON_ERROR_STOP=1 \
  -f src/main/resources/db/manual-migrations/2026-11-20-one-open-loan-per-copy.sql
```

**Verify:**

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
  AND indexname = 'uq_loans_one_open_per_copy';

SELECT book_copy_id, COUNT(*)
FROM loans
WHERE status IN ('REQUESTED', 'ACTIVE', 'LATE')
GROUP BY book_copy_id
HAVING COUNT(*) > 1;

SELECT version, installed_at, installed_by
FROM schema_migrations
WHERE version = '2026-11-20-one-open-loan-per-copy';
```

The duplicate query must return no rows. The index definition must be unique, target only `book_copy_id`, and contain exactly the partial predicate for the three open statuses. The migration fails rather than silently accepting an existing index with the same name but a different definition.

**Rollback:** If rollback is required, first stop writes that can create or change loans, then drop only the index in a reviewed transaction:

```sql
BEGIN;
DROP INDEX IF EXISTS uq_loans_one_open_per_copy;
DELETE FROM schema_migrations
WHERE version = '2026-11-20-one-open-loan-per-copy';
COMMIT;
```

Dropping the index removes database enforcement. Do this only as part of a controlled application rollback and restore the invariant through a forward fix as soon as possible.

## New database provisioning

For a new database:

1. Create an empty PostgreSQL database and a dedicated application role.
2. Configure the production environment variables and a production-strength JWT secret.
3. For the one-time initial bootstrap only, start the application with the `prod` profile and `DB_SCHEMA_INIT_MODE=always`. This explicitly applies `schema.sql` and `data.sql` before Hibernate validates the schema. Do not enable this setting for routine restarts or existing databases.
4. After the first successful bootstrap, remove `DB_SCHEMA_INIT_MODE` (or set it to `never`) and restart the application. The default production behavior is `never`: schema and seed scripts are not executed on startup.
5. Do **not** replay the historical manual migrations. The current `schema.sql` already includes the reservation status and one-open-loan partial unique index.
6. Confirm that the application starts and that the schema checks below pass.

The initial, explicitly enabled schema bootstrap creates the `schema_migrations` table. Its lack of historical rows is expected for a new database. Existing production databases must keep `DB_SCHEMA_INIT_MODE=never`; apply only the applicable reviewed manual migrations before deploying code that depends on them.

## Existing database upgrade procedure

1. Identify the current application release and database schema state.
2. Take and verify a restorable database backup.
3. Confirm the maintenance window and stop or restrict writes if the migration requires it.
4. Inspect `schema_migrations`. If the table does not yet exist, the first applicable manual script creates it.
5. For each migration in filename order, check whether its version is already recorded. If not, execute the script with `ON_ERROR_STOP=1`.
6. Run the migration-specific verification queries and inspect the registry.
7. Deploy the application version that depends on the schema change.
8. Run smoke tests for loan request, approval, cancellation and return, and inspect application logs.
9. Record the release, operator, backup identifier, execution time and verification result in the deployment record.

Do not deploy code that depends on a schema change before the corresponding production migration has completed and been verified.

## Operational safeguards

- Use a dedicated least-privilege migration role where operationally possible; application runtime credentials should not be treated as general-purpose DBA credentials.
- RBAC invalidation triggers are installed by the application using PostgreSQL `CREATE OR REPLACE TRIGGER`. Keep this in-place replacement behavior; dropping and recreating triggers would create a window where RBAC changes could fail to invalidate JWTs.
- Pass a PostgreSQL connection URI to `psql` as `PGURI` (for example, `postgresql://user:password@host:5432/database`). Do not pass the JDBC `jdbc:postgresql://` URL directly to `psql`.
- Keep credentials out of shell history, command output, CI logs and committed files.
- Use `-v ON_ERROR_STOP=1` so `psql` stops on the first SQL error. Transactional scripts must not be marked as applied when they fail.
- Review the exact SQL diff and target database before execution.
- Do not run production migration files automatically at application startup.
- Do not use Hibernate `ddl-auto=update` in production.
- Preserve the SQL scripts as immutable history after they have been applied. Corrections should be delivered as a new forward migration, not by editing a previously executed file.

## Relationship with development and CI

The `dev` profile applies `schema.sql` and the reviewed `2026-11-13-reserve-book-copies.sql` adjustment automatically for local development. The one-open-loan index is part of `schema.sql`. The `test` profile creates isolated schemas through Hibernate and exercises the database constraints in PostgreSQL integration tests.

These development/test behaviors do not change the production procedure: production manual migrations remain operator-controlled and are never run automatically.
