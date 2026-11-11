# 📚 Library API

REST API for library management, developed with **Java 17 + Spring Boot**. The project provides a backend for catalog management, physical book copies, users, authentication, role/permission-based authorization and loan circulation.

The API is developed as a portfolio project with a focus on **layered architecture, automated testing, security, API contracts and continuous integration**.

## Overview

The main capabilities are:

- User registration and authentication
- JWT access and refresh tokens
- Role/permission-based authorization (RBAC)
- Author and book management
- Physical book-copy management
- Loan creation, approval, return and cancellation
- Pagination, sorting and dynamic book filtering
- OpenAPI/Swagger documentation
- PostgreSQL persistence with Hibernate schema management
- Automated tests with a minimum JaCoCo line-coverage gate of 80%
- SonarCloud code-quality analysis
- Docker and Docker Compose support
- Render-compatible production configuration
- Environment-specific CORS configuration

## Stack

| Technology | Purpose |
|---|---|
| Java 17 | Application language and runtime target |
| Spring Boot 3.2.5 | Backend framework |
| Spring Web | REST API |
| Spring Security | Authentication and authorization |
| JWT / JJWT | Access and refresh tokens |
| Spring Data JPA / Hibernate | Persistence and ORM |
| PostgreSQL | Relational database |
| MapStruct | Entity/DTO mapping at the API boundary |
| Springdoc OpenAPI | Swagger/OpenAPI documentation |
| Maven | Build and dependency management |
| JaCoCo | Automated code-coverage measurement |
| SonarCloud | Static analysis and code-quality monitoring |
| Docker / Docker Compose | Local/containerized execution |

## Architecture

The project uses a layered architecture with a clear distinction between API, application, domain and infrastructure responsibilities.

```text
src/main/java/com/jpoltramari/library_api/

├── api/
│   ├── config/         # API configuration properties
│   ├── controller/     # REST endpoints
│   ├── dto/            # Request/response models
│   ├── exception/      # API exception handling and error responses
│   └── mapper/         # Entity/DTO mapping
│
├── application/
│   ├── command/        # Input models for application use cases
│   ├── port/           # Contracts for infrastructure integrations
│   ├── result/         # Application result models
│   └── service/        # Application use cases and orchestration
│
├── domain/
│   ├── enums/          # Domain enumerations
│   ├── exception/      # Business/domain exceptions
│   ├── filter/         # Domain query/filter objects
│   ├── model/          # Domain entities
│   ├── repository/     # Spring Data repository interfaces
│   └── spec/           # Dynamic query specifications
│
└── infrastructure/
    ├── config/         # Technical configuration
    ├── security/       # Authentication, JWT and authorization
    │   ├── filter/
    │   ├── jwt/
    │   ├── rbac/
    │   └── snapshot/
    └── web/            # Web infrastructure adapters
```

### Responsibility boundaries

- **API** handles HTTP endpoints, request validation, response DTOs, OpenAPI documentation, mapping and HTTP error representation.
- **Application** organizes use cases through commands and services, exposes integration contracts through ports, and defines application result models.
- **Domain** contains entities, enums, business exceptions, Spring Data repository interfaces, filters and query specifications.
- **Infrastructure** provides technical configuration and implementations for security, JWT, RBAC, token management, correlation IDs and web integration.

Ports are used where an application use case needs to depend on an external technical capability without coupling directly to its implementation. Other components remain concrete when an abstraction does not provide a practical benefit.

## Security and RBAC

Authentication uses JWT access tokens and refresh tokens. Passwords are protected with BCrypt.

Authorization is permission-based and supports the following groups:

- **USER** — catalog access and own-loan operations
- **LIBRARIAN** — catalog, physical copies and circulation management
- **ADMIN** — administrative operations

Security also includes:

- JWT issuer and audience validation
- Token expiration validation
- Token-version validation
- Refresh-token rotation
- Refresh-token reuse detection
- Token revocation/blacklisting
- Permission-based access control
- Security snapshots for authorization-related state

The JWT secret is interpreted as literal UTF-8 text, not Base64, and must contain at least 32 bytes (256 bits) for HS256. The `dev` profile provides a local-only fallback; `prod` still requires `JWT_SECRET` through the environment. Use a cryptographically random, environment-specific secret in production. Never commit real credentials or secrets.

## Catalog

- Author CRUD
- Book CRUD
- ISBN format and uniqueness validation
- Dynamic book filtering
- Pagination and sorting
- Book creation with an initial physical-copy quantity
- Book cover URL
- Book description/synopsis
- Individual book details
- Total, available and loan-count metadata

### Book response example

```json
{
  "id": 1,
  "isbn": "9780451524935",
  "title": "1984",
  "genre": "SCIENCE_FICTION",
  "createdAt": "2026-08-26T12:00:00",
  "coverUrl": "https://...",
  "description": "Book synopsis...",
  "totalCopies": 3,
  "availableCopies": 2,
  "loanCount": 5,
  "authors": []
}
```

## Physical collection

- Physical copies associated with books
- Unique copy barcodes
- Copy statuses such as `AVAILABLE`, `LOANED` and `MAINTENANCE`
- Quantity creation during book registration
- Total and available quantity calculation
- Copy location and active-state management

## Circulation

- Loan creation
- Loan approval/activation
- Loan return
- Loan cancellation
- Own-loan and all-loan views
- Loan history
- Validation of copy availability and loan business rules

## Database

PostgreSQL is the primary database. Schema initialization is profile-specific so that development convenience does not weaken production safety.

Database initialization is split by responsibility:

- `src/main/resources/schema.sql` contains the PostgreSQL DDL required to provision a new database. Its `CREATE TABLE/INDEX IF NOT EXISTS` statements leave existing tables and their data untouched.
- `src/main/resources/data.sql` initializes the RBAC groups and permissions in every profile. It does not create demo accounts.
- `src/main/resources/data-dev.sql` loads development-only accounts, catalog fixtures, physical copies and loan scenarios. It is executed only with the `dev` profile.

Each environment uses a different schema strategy:

| Profile | Hibernate | SQL initialization |
|---|---|---|
| `dev` | `ddl-auto=update` | Hibernate runs first, followed by `schema.sql`, `data.sql` and `data-dev.sql` |
| `test` | `ddl-auto=create-drop` | Hibernate creates the isolated test schema, then the shared RBAC seed runs |
| `prod` | `ddl-auto=validate` | `schema.sql` and `data.sql` run before Hibernate validates the schema |

The production profile never asks Hibernate to create or update tables. On a new production database, `schema.sql` provisions the initial structure before JPA starts. On an existing database, the idempotent DDL skips existing tables and does not delete application data.

Flyway and its migration history are not used. `schema.sql` is an initial bootstrap, not a versioned migration system. Future production schema changes must be delivered through reviewed, manually executed SQL scripts with an explicit execution record and backup/rollback plan. Do not use `ddl-auto=update` in production.

## Configuration

The application uses Spring profiles for environment-specific configuration:

- `dev` — local development
- `prod` — production deployment

Environment variables override profile defaults. The `dev` profile includes local-only defaults; the `prod` profile still requires `JWT_SECRET` from the environment.

### Environment variables

The repository provides `.env.example` with the main local configuration:

```text
DB_URL=jdbc:postgresql://localhost:5432/library
DB_USER=postgres
DB_PASS=postgres
SPRING_PROFILES_ACTIVE=dev

JWT_SECRET=library-api-local-dev-secret-2026-change-me
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
JWT_ISSUER=library-api
JWT_AUDIENCE=library-api-clients
```

Production additionally supports:

```text
PORT=8080
CORS_ALLOWED_ORIGINS=<frontend-origins>
```

Never commit real credentials or secrets.

## Running locally

### Prerequisites

- JDK 17+
- Maven Wrapper included in the repository
- PostgreSQL, or Docker/Docker Compose

### Run with a local PostgreSQL instance

```bash
git clone https://github.com/JoaoPauloOlt/library-api.git
cd library-api
cp .env.example .env
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

### Run with Docker Compose

Copy `.env.example` to `.env` and run. The sample secret is for local development only; production must use a strong, random UTF-8 secret of at least 32 bytes. Do not Base64-encode it.

```bash
docker compose up --build
```

The API and PostgreSQL services are started together. The API remains available at `http://localhost:8080`.

## Testing and quality

All automated tests are maintained under `src/test/java/com/jpoltramari/library_api/`, organized by API, application, domain and infrastructure responsibility. Test sources are not stored at the repository root.


Run the complete verification lifecycle with:

```bash
./mvnw verify
```

Maven Surefire activates the `test` profile for tests by default. The profile uses `ddl-auto=create-drop` and the isolated `library_test` database (`TEST_DB_URL`). Tests that specifically verify development fixtures use the `dev` profile and `DB_URL`.

The production bootstrap integration test uses a separate, initially empty `library_schema` database (`PROD_SCHEMA_TEST_DB_URL`). For a local PostgreSQL instance, create the three databases before running the full suite:

```sql
CREATE DATABASE library;
CREATE DATABASE library_test;
CREATE DATABASE library_schema;
```

The CI workflow provisions these isolated databases automatically and runs against PostgreSQL 16.

This executes the automated test suite, generates the JaCoCo report and enforces the project coverage gate.

The Maven gate requires:

```text
Global LINE coverage >= 80%
```

The CI workflow also:

- runs against PostgreSQL 16;
- publishes the JaCoCo report as a GitHub Actions artifact;
- prints the global LINE coverage summary;
- executes SonarCloud analysis;
- validates changes before they are merged into `develop`.

The generated JaCoCo report is available at:

```text
target/site/jacoco/
```

## API documentation

When running locally:

**Swagger UI**

```text
http://localhost:8080/swagger-ui/index.html
```

**OpenAPI JSON**

```text
http://localhost:8080/v3/api-docs
```

### Authentication in Swagger

1. Call `POST /auth/login`.
2. Obtain the JWT access token from the response.
3. In Swagger UI, select **Authorize**.
4. Enter only the token.
5. Swagger UI sends the token using the Bearer authentication scheme.

Protected endpoints require authentication and, where applicable, the required RBAC permission.

## Production

The API is configured for cloud deployment and can run on Render using the `prod` Spring profile.

Production configuration includes:

- Dynamic `PORT` support
- PostgreSQL through environment variables
- Environment-specific CORS configuration
- JWT configuration through environment variables
- Correlation ID logging

The frontend integration is maintained separately in the `library-web` project.

## Development workflow

The project follows a controlled GitHub workflow:

```text
Issue
  ↓
Branch
  ↓
Implementation
  ↓
CI
  ↓
Pull Request
  ↓
Review
  ↓
Merge into develop
  ↓
Close Issue
```

Development branches should be short-lived and focused on a single change.

Examples:

```text
feature/<description>
fix/<description>
refactor/<description>
quality/<description>
docs/<description>
```

Changes should not be mixed across unrelated Pull Requests. CI must pass before a Pull Request is merged.

## Repository goals

The project is being developed incrementally, prioritizing:

- Stable REST contracts
- Secure authentication and authorization
- Clear application/domain boundaries
- Automated testing
- Measurable code coverage
- Static code-quality analysis
- Consistent Hibernate schema management and seed initialization
- Controlled integration with the frontend
- Production-ready configuration

The architecture is intentionally evolved according to concrete problems identified in the codebase rather than by introducing patterns or abstractions without a practical need.
