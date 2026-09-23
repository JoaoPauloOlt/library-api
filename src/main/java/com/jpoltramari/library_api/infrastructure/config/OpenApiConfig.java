package com.jpoltramari.library_api.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Library API",
                version = "1.0.0",
                description = """
                        REST API for managing books, authors, physical copies, users and loans.

                        Authentication:
                        - Use POST /auth/login to obtain an access token.
                        - Use the Authorize button in Swagger UI with the returned JWT.
                        - The bearer token is required for protected endpoints.
                        """,
                contact = @Contact(
                        name = "Library API",
                        url = "https://github.com/JoaoPauloOlt/library-api"
                )
        ),
        servers = {
                @Server(url = "/", description = "Current server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "JWT access token. Enter only the token value; Swagger UI adds the Bearer prefix automatically."
)
public class OpenApiConfig {
}
