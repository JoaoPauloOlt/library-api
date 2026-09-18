package com.jpoltramari.library_api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshot;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecurityInfrastructureBehaviorTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthorizationHeaderOrWithEmptyBearerToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenVersionValidator validator = mock(TokenVersionValidator.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, validator);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest missing = new MockHttpServletRequest("GET", "/books");
        filter.doFilterInternal(missing, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(missing, any());

        MockHttpServletRequest empty = new MockHttpServletRequest("GET", "/books");
        empty.addHeader("Authorization", "Bearer   ");
        filter.doFilterInternal(empty, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(empty, any());

        verifyNoInteractions(jwtService, validator);
    }

    @Test
    void shouldAuthenticateValidBearerToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenVersionValidator validator = mock(TokenVersionValidator.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, validator);
        FilterChain chain = mock(FilterChain.class);

        JwtClaims claims = claims(42L);
        when(jwtService.parseAndValidate("token")).thenReturn(Optional.of(claims));
        when(validator.validate(claims))
                .thenReturn(TokenVersionValidator.ValidationResult.VALID);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(JwtAuthenticationToken.class);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateInvalidTokenVersion() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenVersionValidator validator = mock(TokenVersionValidator.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, validator);
        FilterChain chain = mock(FilterChain.class);

        JwtClaims claims = claims(42L);
        when(jwtService.parseAndValidate("token")).thenReturn(Optional.of(claims));
        when(validator.validate(claims))
                .thenReturn(TokenVersionValidator.ValidationResult.VERSION_MISMATCH);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        request.addHeader("Authorization", "Bearer token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSkipJwtProcessingWhenAuthenticationAlreadyExists() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        TokenVersionValidator validator = mock(TokenVersionValidator.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, validator);
        FilterChain chain = mock(FilterChain.class);

        Authentication existing = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existing);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(request, any());
        verifyNoInteractions(jwtService, validator);
    }

    @Test
    void shouldWriteUnauthorizedAndForbiddenSecurityResponses() throws Exception {
        ApiSecurityExceptionHandler handler = new ApiSecurityExceptionHandler(new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/books");
        MockHttpServletResponse unauthorized = new MockHttpServletResponse();
        handler.commence(request, unauthorized, new AuthenticationException("missing token") {});

        assertThat(unauthorized.getStatus()).isEqualTo(401);
        assertThat(unauthorized.getContentType()).startsWith("application/json");
        assertThat(unauthorized.getContentAsString()).contains("AUTHENTICATION_REQUIRED");

        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        handler.handle(request, forbidden, new AccessDeniedException(null));

        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(forbidden.getContentAsString()).contains("ACCESS_DENIED");
        assertThat(forbidden.getContentAsString()).contains("Insufficient permissions");
    }

    @Test
    void shouldLoadUserDetailsOrRejectUnknownUser() {
        var repository = mock(com.jpoltramari.library_api.domain.repository.UserRepository.class);
        var resolver = new com.jpoltramari.library_api.infrastructure.security.rbac.RbacResolver();
        var service = new UserDetailsServiceImpl(repository, resolver);

        var user = new com.jpoltramari.library_api.domain.model.User();
        user.setId(1L);
        user.setName("Library User");
        user.setEmail("user@library.com");
        user.setPassword("encoded");
        user.setStatus(com.jpoltramari.library_api.domain.enums.UserStatus.ACTIVE);

        when(repository.findByEmailWithGroupsAndPermissions("user@library.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@library.com");
        assertThat(details.getUsername()).isEqualTo("user@library.com");
        assertThat(details.isEnabled()).isTrue();

        when(repository.findByEmailWithGroupsAndPermissions("missing@library.com"))
                .thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.loadUserByUsername("missing@library.com")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: missing@library.com");
    }

    private JwtClaims claims(Long userId) {
        return new JwtClaims(
                "jti", userId, "user@library.com", "Library User",
                List.of("USER"), List.of("BOOK_READ"), 0,
                Instant.now(), Instant.now().plusSeconds(3600),
                "library-api", "library-api-clients"
        );
    }
}
