#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"
if [[ "$PROFILE" != "dev" ]]; then
    echo "Refusing to reset: SPRING_PROFILES_ACTIVE must be exactly 'dev' (received: '$PROFILE')." >&2
    exit 1
fi

DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/library}"
DB_USER="${DB_USER:-postgres}"
DB_PASS="${DB_PASS:-postgres}"

if [[ ! "$DB_URL" =~ ^jdbc:postgresql://(localhost|127[.]0[.]0[.]1)(:([0-9]+))?/([^/?]+)([?].*)?$ ]]; then
    echo "Refusing to reset: DB_URL must point to a local PostgreSQL database on localhost or 127.0.0.1." >&2
    echo "Current DB_URL: $DB_URL" >&2
    exit 1
fi

DB_HOST="${BASH_REMATCH[1]}"
DB_PORT="${BASH_REMATCH[3]:-5432}"
DB_NAME="${BASH_REMATCH[4]}"

case "$DB_NAME" in
    postgres|template0|template1|library_test|library_schema)
        echo "Refusing to reset protected database '$DB_NAME'." >&2
        exit 1
        ;;
esac

if ! command -v psql >/dev/null 2>&1; then
    echo "PostgreSQL client 'psql' was not found. Install the PostgreSQL client and try again." >&2
    exit 1
fi

for sql_file in schema.sql data.sql data-dev.sql; do
    if [[ ! -f "$ROOT_DIR/src/main/resources/$sql_file" ]]; then
        echo "Required SQL file not found: $ROOT_DIR/src/main/resources/$sql_file" >&2
        exit 1
    fi
done

echo "WARNING: this operation will erase all data in the public schema of:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
read -r -p "Type the database name '$DB_NAME' to confirm: " confirmation

if [[ "$confirmation" != "$DB_NAME" ]]; then
    echo "Reset cancelled."
    exit 1
fi

echo "Resetting development database '$DB_NAME'..."

PGPASSWORD="$DB_PASS" psql \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --dbname "$DB_NAME" \
    --no-password \
    --set ON_ERROR_STOP=1 \
    --single-transaction \
    --command "DROP SCHEMA public CASCADE; CREATE SCHEMA public;" \
    --file "$ROOT_DIR/src/main/resources/schema.sql" \
    --file "$ROOT_DIR/src/main/resources/data.sql" \
    --file "$ROOT_DIR/src/main/resources/data-dev.sql"

echo "Development database '$DB_NAME' restored successfully."
echo "Start the API with the 'dev' profile to continue."
