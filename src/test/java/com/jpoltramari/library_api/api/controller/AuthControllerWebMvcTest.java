package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.application.result.auth.AuthenticationResult;
import com.jpoltramari.library_api.application.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class AuthControllerWebMvcTest {
    private static final String CSRF_TOKEN = "csrf-test-token";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthService service;

    @Test
    void shouldIssueCsrfTokenAndCookieForBrowserClient() throws Exception {
        mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")));
    }

    @Test
    void shouldLoginAndSetHttpOnlyRefreshCookieWithoutReturningRefreshToken() throws Exception {
        when(service.login("user@library.com", "User123!"))
                .thenReturn(new AuthenticationResult("access-token", "refresh-token", 3600L));
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
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refreshToken=refresh-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Path=/auth"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"))));
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
    void shouldRefreshUsingCookieTrustedOriginAndCsrfAndRotateCookie() throws Exception {
        when(service.refresh("refresh-token"))
                .thenReturn(new AuthenticationResult("new-access-token", "rotated-refresh-token", 1800L));
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token"), new Cookie("XSRF-TOKEN", CSRF_TOKEN))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=rotated-refresh-token")));
        verify(service).refresh("refresh-token");
    }

    @Test
    void shouldRejectRefreshWithoutCsrfHeader() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token"))
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectRefreshWithoutTrustedOrigin() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-token"), new Cookie("XSRF-TOKEN", CSRF_TOKEN))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("XSRF-TOKEN", CSRF_TOKEN))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutAndClearRefreshAndCsrfCookies() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "refresh-token"), new Cookie("XSRF-TOKEN", CSRF_TOKEN))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(service).logout("refresh-token", null);
    }
}
