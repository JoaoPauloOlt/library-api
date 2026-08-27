# 📚 Library API

REST API for library management, developed with **Java 17 + Spring Boot**. The project is a portfolio-grade library system with authentication, RBAC, catalog management, physical book copies and loan circulation.

## Stack

- Java 17
- Spring Boot
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven
- Docker / Docker Compose
- OpenAPI / Swagger

## Architecture

```text
api/
  controller/ dto/ exception/ mapper/
domain/
  model/ enums/ repository/ service/ specification/
infrastructure/
  security/ config/
```

The application follows layered architecture and keeps business rules in the domain services.

## Security and RBAC

Authentication uses JWT and passwords are protected with BCrypt. Authorization is permission-based:

- **USER** — catalog and own-loan operations
- **LIBRARIAN** — catalog, physical copies and circulation management
- **ADMIN** — system administration

Never commit real credentials, JWT secrets or database credentials. Use environment variables.

## Catalog

- Author CRUD
- Book CRUD
- ISBN validation and uniqueness
- Dynamic filtering and pagination
- Book creation with physical-copy quantity
- Book cover URL
- Book description/synopsis
- `GET /books/{id}` for individual book details
- Total, available and loan-count metadata

### Book model

A book exposes, among other fields:

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

- Book copies with unique barcodes
- Copy statuses such as `AVAILABLE`, `LOANED` and `MAINTENANCE`
- Quantity creation during book registration
- Total and available quantity calculation

## Circulation

- Loan creation
- Approval/activation workflow
- Loan return and cancellation
- Own-loan and all-loan views
- Loan history
- Business-rule validation

## Database and migrations

PostgreSQL is the database. Schema changes are versioned with Flyway under:

```text
src/main/resources/db/migration
```

The migration chain currently includes `V1` through `V6`, `V8`, `V9` and `V10`. **Never modify an already executed migration**; introduce the next version instead.

`V10__add_book_description.sql` adds the nullable `description` column to `books` for the individual book details page.

## Running locally

```bash
git clone https://github.com/JoaoPauloOlt/library-api.git
cd library-api
cp .env.example .env
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080` by default.

## Testing

```bash
./mvnw test
```

The CI validates compilation and automated tests before changes are merged into `develop`.

## API documentation

When running locally, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Development workflow

Use short-lived feature/fix branches, descriptive commits, CI validation and Pull Requests targeting `develop`. Database changes must always use a new Flyway version.
