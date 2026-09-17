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
                description = "REST API for managing books, authors, physical copies, users and loans in a library.",
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
        description = "JWT access token. Use the value returned by the authentication endpoint."
)
public class OpenApiConfig {
}
