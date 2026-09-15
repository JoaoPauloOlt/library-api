package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.domain.service.AuthService;
import com.jpoltramari.library_api.infrastructure.security.ApiSecurityExceptionHandler;
import com.jpoltramari.library_api.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        com.jpoltramari.library_api.infrastructure.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class
})
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService service;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldLoginThroughPublicEndpoint() throws Exception {
        when(service.login("user@library.com", "User123!"))
                .thenReturn(new LoginResponse("access-token", "refresh-token", 3600L));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "user@library.com",
                                  "password": "User123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void shouldRejectInvalidLoginPayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLogoutAndReturnNoContent() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "refresh-token",
                                  "accessToken": "access-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(service).logout("refresh-token", "access-token");
    }
}
