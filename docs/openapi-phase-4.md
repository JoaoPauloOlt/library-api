# OpenAPI / Swagger

The API publishes its OpenAPI 3 specification through Springdoc.

## Local endpoints

With the API running on the default port:

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

The port follows the active runtime configuration. In production, the server uses the platform-provided `PORT` variable (defaulting to `8080`).

## Authentication

1. Execute `POST /auth/login` with valid credentials.
2. Copy the returned access token (not the refresh token).
3. Select **Authorize** in Swagger UI.
4. Paste only the access-token value. The configured HTTP Bearer scheme adds the `Bearer` prefix.
5. Execute protected operations with the authenticated user's permissions.

The OpenAPI security scheme is named `bearerAuth`. Public authentication operations are documented without a global requirement; protected controllers and operations declare the scheme and their permission requirements in their descriptions and response metadata.

## Contract coverage

The specification is generated from the application annotations. It includes controller operations, query/path parameters, request and response DTO schemas, validation metadata, pagination models, common HTTP response codes and the standardized `ErrorResponse` schema.

The generated document at `/v3/api-docs` is the source of truth for the running build. This Markdown guide is operational documentation and must not be treated as a manually maintained substitute for the generated contract.

## Validation before release

For a release candidate:

1. Start the application with a working PostgreSQL database and RabbitMQ broker (or explicitly disable messaging for a broker-independent local inspection).
2. Open Swagger UI and confirm the document loads.
3. Retrieve `/v3/api-docs` and verify the API metadata reports the release version.
4. Check representative public, authenticated and permission-protected operations.
5. Confirm the documented request/response schemas match the DTOs in the same commit.
