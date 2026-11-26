package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.auth.LoginInput;
import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.application.result.auth.AuthenticationResult;
import com.jpoltramari.library_api.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT authentication and session management")
public class AuthController {
    private static final String REFRESH_COOKIE = "refreshToken";
    private final AuthService service;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CsrfTokenRepository csrfTokenRepository;

    @Value("${app.security.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;
    @Value("${app.security.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;
    @Value("${app.security.refresh-cookie-max-age-seconds:86400}")
    private long refreshCookieMaxAgeSeconds;

    @GetMapping("/csrf")
    @Operation(summary = "Get CSRF token", description = "Initializes the browser CSRF cookie and returns its matching request token.")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @Operation(summary = "Authenticate user", description = "Returns an access token and stores the refresh token in an HttpOnly cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginInput input, HttpServletResponse response) {
        AuthenticationResult result = service.login(input.email(), input.password());
        setRefreshCookie(response, result.refreshToken());
        return toLoginResponse(result);
    }

    @Operation(summary = "Refresh access token", description = "Rotates the HttpOnly refresh cookie and returns a new access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public LoginResponse refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                 HttpServletRequest request, HttpServletResponse response) {
        requireTrustedOrigin(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is unavailable");
        }
        AuthenticationResult result = service.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return toLoginResponse(result);
    }

    @Operation(summary = "Logout", description = "Invalidates the refresh cookie and current access token session.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout completed successfully"),
            @ApiResponse(responseCode = "403", description = "Untrusted request origin")
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                       @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                       HttpServletRequest request, HttpServletResponse response) {
        requireTrustedOrigin(request);
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim() : null;
        try {
            service.logout(refreshToken, accessToken);
        } finally {
            clearRefreshCookie(response);
            csrfTokenRepository.saveToken(null, request, response);
        }
    }

    private void requireTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);
        if (origin == null || origin.isBlank() || configuration == null
                || configuration.checkOrigin(origin) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Untrusted request origin");
        }
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ofSeconds(Math.max(1, refreshCookieMaxAgeSeconds)))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private LoginResponse toLoginResponse(AuthenticationResult result) {
        return new LoginResponse(result.accessToken(), result.expiresIn());
    }
}
