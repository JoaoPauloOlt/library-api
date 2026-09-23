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
- Pagination and dynamic book filtering
- OpenAPI/Swagger documentation
- PostgreSQL persistence with Flyway migrations
- Automated tests with a minimum JaCoCo line-coverage gate of 80%
- SonarCloud code-quality analysis
- Docker support
- Render-compatible production configuration

## Stack

| Technology | Purpose |
|---|---|
| Java 17 | Application language |
| Spring Boot 3.2.5 | Backend framework |
| Spring Web | REST API |
| Spring Security | Authentication and authorization |
| JWT / JJWT | Access and refresh tokens |
| Spring Data JPA / Hibernate | Persistence and ORM |
| PostgreSQL | Relational database |
| Flyway | Database versioning and migrations |
| MapStruct | Entity/DTO mapping at the API boundary |
| Springdoc OpenAPI | Swagger/OpenAPI documentation |
| Maven | Build and dependency management |
| JaCoCo | Automated code-coverage measurement |
| SonarCloud | Static analysis and code-quality monitoring |
| Docker / Docker Compose | Local/containerized execution |

## Architecture

The project uses a layered architecture with a clear distinction between the API, application, domain and infrastructure responsibilities.

```text
src/main/java/com/jpoltramari/library_api/

├── api/
│   ├── controller/     # HTTP endpoints
│   ├── dto/            # Request/response models
│   ├── exception/      # HTTP error representation
│   └── mapper/         # API/entity mapping
│
├── application/
│   └── service/        # Application use cases and orchestration
│
├── domain/
│   ├── model/          # Domain entities
│   ├── enums/          # Domain enumerations
│   ├── exception/      # Business/domain exceptions
│   ├── repository/     # Persistence contracts used by the application
│   ├── filter/         # Domain query/filter objects
│   └── specification/  # Dynamic query specifications
│
└── infrastructure/
    └── security/       # JWT, RBAC and security infrastructure
```

### Responsibility boundaries

- **API** handles HTTP, request validation, response models, OpenAPI documentation and API/entity mapping.
- **Application** contains the use cases and coordinates repositories, domain objects and required infrastructure services.
- **Domain** contains entities, business exceptions, enums and query-related domain structures.
- **Infrastructure** contains technical implementations such as JWT authentication, token management and security integrations.

The current architecture intentionally avoids unnecessary abstractions such as interfaces, ports or duplicated application DTOs when they do not provide a concrete benefit.

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

Never commit real credentials, JWT secrets or database credentials. Use environment variables.

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

## Database and migrations

PostgreSQL is the primary database.

Flyway migrations are stored under:

```text
src/main/resources/db/migration
```

**Never modify an already executed migration.** Create the next migration version for schema changes.

## Configuration

The application uses Spring profiles for environment-specific configuration:

- `dev` — local development
- `prod` — production deployment

Important configuration is supplied through environment variables rather than committed secrets.

Typical production variables include:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=<JDBC PostgreSQL URL>
DB_USER=<database user>
DB_PASS=<database password>
JWT_SECRET=<strong secret>
JWT_EXPIRATION=<access-token lifetime>
JWT_REFRESH_EXPIRATION=<refresh-token lifetime>
JWT_ISSUER=<issuer>
JWT_AUDIENCE=<audience>
CORS_ALLOWED_ORIGINS=<frontend origins>
```

For local development, use the project's `.env.example`/environment configuration and never commit real credentials.

## Running locally

### Prerequisites

- JDK 17+
- Maven Wrapper included in the repository
- PostgreSQL, unless using the provided container setup
- Docker, if running the application/database through containers

### Run the application

```bash
git clone https://github.com/JoaoPauloOlt/library-api.git
cd library-api
./mvnw spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

The active Spring profile and database configuration can be supplied through environment variables.

## Testing and quality

Run the complete verification lifecycle with:

```bash
./mvnw verify
```

This executes the automated test suite, generates the JaCoCo report and enforces the project coverage gate.

The current Maven gate requires:

```text
Global LINE coverage >= 80%
```

The CI workflow also:

- publishes the JaCoCo report as a GitHub Actions artifact;
- prints the global LINE coverage summary;
- executes SonarCloud analysis;
- validates changes before they are merged into `develop`.

The generated JaCoCo report is available at:

```text
target/site/jacoco/
```

## API documentation

When running locally, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Authentication flow:

1. Call `POST /auth/login`.
2. Obtain the JWT access token from the response.
3. In Swagger UI, select **Authorize**.
4. Enter only the token.
5. Swagger UI sends the token using the Bearer authentication scheme.

Protected endpoints require authentication and, where applicable, the required RBAC permission.

## Production

The API is configured for cloud deployment and can run on Render using the production Spring profile.

Production configuration includes:

- Dynamic `PORT` support
- PostgreSQL through environment variables
- Flyway migrations on startup
- Environment-specific CORS configuration
- JWT configuration through environment variables

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
- Maintainable database migrations
- Controlled integration with the frontend
- Production-ready configuration

The architecture is intentionally evolved according to concrete problems identified in the codebase rather than by introducing patterns or abstractions without a practical need.
