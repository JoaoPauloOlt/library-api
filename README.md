# 📚 Library API

REST API for library management, developed with **Java 17 + Spring Boot**. The project is being built as a portfolio-grade MVP, with authentication, authorization, catalog management, physical book copies and loan circulation.

## 🧰 Stack

- Java 17
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven
- Docker / Docker Compose
- OpenAPI / Swagger

## 🏗️ Architecture

The application follows a layered architecture with clear separation between API, domain and infrastructure concerns:

```text
api/
  controller/
  dto/
  exception/
  mapper/

domain/
  model/
  enums/
  repository/
  service/
  specification/

infrastructure/
  security/
  config/
```

## 🔐 Security

Authentication uses JWT with Spring Security. Passwords are protected with BCrypt and authorization is based on RBAC permissions.

The intended roles are:

- **USER** — catalog and own-loan operations
- **LIBRARIAN** — catalog, copies and circulation management
- **ADMIN** — system administration

Never commit real credentials or JWT secrets. Development and production secrets are supplied through environment variables.

## 📦 Core features

### Catalog

- Author CRUD
- Book CRUD
- ISBN validation and uniqueness
- Dynamic book filtering
- Pagination

### Physical collection

- Book copy management
- Unique barcode per copy
- Copy status (`AVAILABLE`, `LOANED`, `MAINTENANCE`)
- Location tracking
- Total and available quantity

### Circulation

- Loan creation
- Loan return
- Loan history
- Business-rule validation

### Identity and access

- User registration
- JWT authentication
- Refresh tokens
- Logout/session invalidation
- Role/permission based authorization

## 🗄️ Database

PostgreSQL is the project's database. Schema changes are versioned with Flyway migrations under:

```text
src/main/resources/db/migration
```

Current migrations cover the core schema, library domain, circulation, RBAC and refresh tokens.

## 🚀 Running locally

### Option 1 — Docker Compose

```bash
git clone https://github.com/JoaoPauloOlt/library-api.git
cd library-api

cp .env.example .env
# Edit .env and set JWT_SECRET

docker compose up --build
```

The API will be available at `http://localhost:8080`.

### Option 2 — Local Maven + PostgreSQL

Create a PostgreSQL database named `library`, configure the variables from `.env.example`, and run:

```bash
./mvnw spring-boot:run
```

## 📖 API documentation

When the application is running, OpenAPI documentation is available through Springdoc/Swagger UI.

```text
http://localhost:8080/swagger-ui/index.html
```

## 🧪 Testing

The project is being expanded with unit and integration tests as part of the MVP quality phase.

```bash
./mvnw test
```

## 🗺️ MVP roadmap

- [x] Layered API architecture
- [x] PostgreSQL + Flyway
- [x] JWT authentication
- [x] RBAC foundation
- [x] Book copies model and API
- [ ] Complete circulation workflow
- [ ] Automated unit tests
- [ ] Integration tests with Testcontainers
- [ ] CI/CD
- [ ] React frontend
- [ ] Production deployment

## 👨‍💻 Author

Developed by **João Paulo Oltramari** as a personal software engineering project focused on backend development with Java and Spring Boot.
