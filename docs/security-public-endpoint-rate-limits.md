# Public endpoint rate limits

The API applies application-level limits to the public login and registration endpoints.

| Endpoint | Key | Default limit | Response when limited |
|---|---|---:|---|
| `POST /auth/login` | Client IP | 30 requests per minute | `429 Too Many Requests`, `Retry-After` reports remaining window seconds |
| `POST /auth/login` | Normalized account email, HMAC-derived | 5 failed attempts per 15 minutes | `429 Too Many Requests`, `Retry-After` reports remaining window seconds |
| `POST /users` | Client IP | 5 requests per hour | `429 Too Many Requests`, `Retry-After` reports remaining window seconds |

Successful login clears the failed-attempt counter for that account. Account identifiers are normalized and HMAC-derived using the configured JWT secret; the raw email is not stored in the limiter key.

## Configuration

The thresholds and windows can be overridden using the environment variables listed in `application.properties`:

- `LOGIN_IP_RATE_LIMIT_MAX_ATTEMPTS`
- `LOGIN_IP_RATE_LIMIT_WINDOW`
- `LOGIN_ACCOUNT_RATE_LIMIT_MAX_FAILED_ATTEMPTS`
- `LOGIN_ACCOUNT_RATE_LIMIT_WINDOW`
- `REGISTRATION_IP_RATE_LIMIT_MAX_ATTEMPTS`
- `REGISTRATION_IP_RATE_LIMIT_WINDOW`

Duration values use ISO-8601 format, for example `PT1M` or `PT15M`. The `Retry-After` header is rounded up to the number of seconds remaining in the active configured window; it is not a fixed value.

## Operational limitations

The limiter is in-memory and process-local. Counters reset on application restart and are not shared between API instances. For multi-instance deployments, replace or back the limiter with a shared atomic store such as Redis.

The IP key is taken from the servlet request's remote address. Do not trust arbitrary client-supplied `X-Forwarded-For` values. If the API runs behind a reverse proxy, configure trusted forwarded-header handling only when the proxy removes untrusted incoming forwarding headers and writes its own.

Registration hashes the supplied password and performs the common default-group lookup before checking email uniqueness, reducing timing differences between existing and new addresses; this is a mitigation, not a guarantee of constant-time database behavior. The account threshold temporarily throttles authentication for that normalized email after repeated invalid-credential failures. Authentication infrastructure errors are not counted as password failures. This is an intentional brute-force control, but it can also be abused to temporarily delay a legitimate user's login.
