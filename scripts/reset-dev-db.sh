#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

# Read only the database/profile keys needed by this helper. Do not source .env:
# sourcing would execute arbitrary shell commands from a local configuration file.
load_dotenv() {
    local env_file="$1"
    local line key value first_char last_char

    [[ -f "$env_file" ]] || return 0

    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        line="${line#"${line%%[![:space:]]*}"}"
        [[ -z "$line" || "$line" == \#* ]] && continue
        [[ "$line" == *=* ]] || continue

        key="${line%%=*}"
        value="${line#*=}"
        key="${key#"${key%%[![:space:]]*}"}"
        key="${key%"${key##*[![:space:]]}"}"

        case "$key" in
            DB_URL|DB_USER|DB_PASS|SPRING_PROFILES_ACTIVE) ;;
            *) continue ;;
        esac

        value="${value#"${value%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        if [[ ${#value} -ge 2 ]]; then
            first_char="${value:0:1}"
            last_char="${value: -1}"
            if [[ "$first_char" == '"' && "$last_char" == '"' ]] || \
               [[ "$first_char" == "'" && "$last_char" == "'" ]]; then
                value="${value:1:${#value}-2}"
            fi
        fi

        case "$key" in
            DB_URL) [[ -n "${DB_URL:-}" ]] || DB_URL="$value" ;;
            DB_USER) [[ -n "${DB_USER:-}" ]] || DB_USER="$value" ;;
            DB_PASS) [[ -n "${DB_PASS:-}" ]] || DB_PASS="$value" ;;
            SPRING_PROFILES_ACTIVE) [[ -n "${SPRING_PROFILES_ACTIVE:-}" ]] || SPRING_PROFILES_ACTIVE="$value" ;;
        esac
    done < "$env_file"
}

load_dotenv "$ROOT_DIR/.env"

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
DB_NAME_LOWER="${DB_NAME,,}"

case "$DB_NAME_LOWER" in
    postgres|template0|template1|library_test|library_schema|prod|production|library_prod|library_production)
        echo "Refusing to reset protected database '$DB_NAME'." >&2
        exit 1
        ;;
esac

# docker-compose.yml names its PostgreSQL container library-postgres and runs
# the API with the prod profile. Never reset that database through localhost.
if command -v docker >/dev/null 2>&1; then
    running_containers="$(docker ps --format '{{.Names}}' 2>/dev/null || true)"
    if grep -Fxq 'library-postgres' <<< "$running_containers"; then
        echo "Refusing to reset: the 'library-postgres' Docker Compose container is running." >&2
        echo "Stop the Compose stack before resetting a local development database." >&2
        exit 1
    fi
fi

if ! command -v psql >/dev/null 2>&1; then
    echo "PostgreSQL client 'psql' was not found. Install the PostgreSQL client and try again." >&2
    exit 1
fi

for required_file in \
    "$ROOT_DIR/src/main/resources/schema.sql" \
    "$ROOT_DIR/scripts/reset-dev-db.sql" \
    "$ROOT_DIR/src/main/resources/data.sql" \
    "$ROOT_DIR/src/main/resources/data-dev.sql"; do
    if [[ ! -f "$required_file" ]]; then
        echo "Required SQL file not found: $required_file" >&2
        exit 1
    fi
done

expected_confirmation="RESET $DB_NAME"
echo "WARNING: this operation will erase application data from the local database:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "The schema objects are preserved; application tables are truncated and reseeded."
read -r -p "Type '$expected_confirmation' to continue: " confirmation

if [[ "$confirmation" != "$expected_confirmation" ]]; then
    echo "Reset cancelled. No database changes were made."
    exit 1
fi

echo "Resetting development database '$DB_NAME'..."

PGPASSWORD="$DB_PASS" psql \
    --no-psqlrc \
    --no-password \
    --set ON_ERROR_STOP=1 \
    --single-transaction \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USER" \
    --dbname "$DB_NAME" \
    --file "$ROOT_DIR/src/main/resources/schema.sql" \
    --file "$ROOT_DIR/scripts/reset-dev-db.sql" \
    --file "$ROOT_DIR/src/main/resources/data.sql" \
    --file "$ROOT_DIR/src/main/resources/data-dev.sql"

echo "Development database '$DB_NAME' restored successfully."
echo "Start the API with the 'dev' profile to continue."
