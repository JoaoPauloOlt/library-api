package com.jpoltramari.library_api.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTRATION_PATH = "/users";
    private static final String RATE_LIMIT_BODY = """
            {"status":429,"title":"Too many requests","detail":"Request rate limit exceeded. Please try again later.","errorCode":"RATE_LIMIT_EXCEEDED"}
            """;

    private final PublicRateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod())) {
            String path = request.getServletPath();
            String clientIp = request.getRemoteAddr();

            if (LOGIN_PATH.equals(path) && !rateLimitService.allowLoginFromIp(clientIp)) {
                reject(response, 60);
                return;
            }

            if (REGISTRATION_PATH.equals(path) && !rateLimitService.allowRegistrationFromIp(clientIp)) {
                reject(response, 3600);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(RATE_LIMIT_BODY);
    }
}
