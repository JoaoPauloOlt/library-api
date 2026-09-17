# Phase 4 OpenAPI documentation

This document records the OpenAPI documentation scope introduced in Phase 4.5.

- OpenAPI metadata and JWT bearer authentication scheme are configured centrally.
- Controllers document their operations, parameters, security requirements and common HTTP outcomes.
- Request/response DTOs expose field descriptions, examples and validation constraints.
- Pagination is represented by `PageResponse`.
- Book filtering is exposed as query parameters through `BookFilter`.
- Standardized `ErrorResponse` is available as the API error schema.

The generated specification is available at `/v3/api-docs` and the Swagger UI is exposed by Springdoc according to the existing application security configuration.
