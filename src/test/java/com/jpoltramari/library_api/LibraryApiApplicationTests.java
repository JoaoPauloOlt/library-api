package com.jpoltramari.library_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "strict-origin-when-cross-origin"
                ));
    }

    @Test
    void openApiDocumentExposesProjectMetadataAndJwtScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Library API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void openApiDocumentExposesExpectedResourcePathsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post").exists())
                .andExpect(jsonPath("$.paths['/auth/logout'].post").exists())
                .andExpect(jsonPath("$.paths['/authors'].get").exists())
                .andExpect(jsonPath("$.paths['/books'].get").exists())
                .andExpect(jsonPath("$.paths['/books/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/books/{bookId}/copies'].get").exists())
                .andExpect(jsonPath("$.paths['/books/{bookId}/copies/availability'].get").exists())
                .andExpect(jsonPath("$.paths['/loans'].get").exists())
                .andExpect(jsonPath("$.paths['/loans/my'].get").exists())
                .andExpect(jsonPath("$.paths['/users'].get").exists())
                .andExpect(jsonPath("$.paths['/users'].post").exists())
                .andExpect(jsonPath("$.components.schemas.BookInput.properties.isbn.pattern").value("\\d{13}"))
                .andExpect(jsonPath("$.components.schemas.BookInput.properties.quantity.minimum").value(0))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.correlationId").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse.properties.totalElements").exists());
    }

    @Test
    void userRegistrationIsDocumentedAsPublicWhileAdministrationRequiresJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/users'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/users'].get.security[0].bearerAuth").exists());
    }
}
