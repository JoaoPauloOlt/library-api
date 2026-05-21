package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenVersionValidator tokenVersionValidator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        jwtService.parseAndValidate(token)
                .flatMap(claims -> validateAndBuild(claims))
                .ifPresent(authentication ->
                        SecurityContextHolder.getContext().setAuthentication(authentication)
                );

        chain.doFilter(request, response);
    }

    private java.util.Optional<JwtAuthenticationToken> validateAndBuild(JwtClaims claims) {
        TokenVersionValidator.ValidationResult result = tokenVersionValidator.validate(claims);
        if (result != TokenVersionValidator.ValidationResult.VALID) {
            return java.util.Optional.empty();
        }

        AuthenticatedUser principal = AuthenticatedUser.fromClaims(claims);
        log.debug("event=jwt_authenticated userId={} correlationId={}",
                claims.userId(), MDC.get(CorrelationIdFilter.MDC_KEY));
        return java.util.Optional.of(new JwtAuthenticationToken(principal, claims));
    }
}
