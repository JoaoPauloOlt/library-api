package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.exception.LoginRateLimitException;
import com.jpoltramari.library_api.application.port.security.AccessTokenPort;
import com.jpoltramari.library_api.application.port.security.RefreshTokenPort;
import com.jpoltramari.library_api.application.result.auth.AuthenticationResult;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import com.jpoltramari.library_api.infrastructure.security.PublicRateLimitService;
import org.springframework.security.core.Authentication;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private AccessTokenPort accessTokenPort;
    @Mock private RefreshTokenPort refreshTokenPort;
    @Mock private UserRepository userRepository;
    @Mock private PublicRateLimitService rateLimitService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, accessTokenPort, refreshTokenPort, userRepository, rateLimitService);
    }

    @Test
    void shouldLoginAndIssueTokens() {
        User user = new User();
        var issued = new RefreshTokenPort.IssuedTokens("access", "refresh");
        when(userRepository.findByEmailWithGroupsAndPermissions("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenPort.issueTokens(user)).thenReturn(issued);
        when(accessTokenPort.getExpiration()).thenReturn(900L);

        AuthenticationResult response = service.login("  JOHN@Example.COM ", "password123");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertEquals("john@example.com", authenticationCaptor.getValue().getName());
        verify(rateLimitService).resetFailedLogins("john@example.com");
    }

    @Test
    void shouldRecordFailedLoginAttempt() {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class,
                () -> service.login("  John@Example.com ", "wrong-password"));

        verify(rateLimitService).recordFailedLogin("john@example.com");
    }

    @Test
    void shouldRejectLoginWhenAccountIsBlockedBeforePasswordVerification() {
        when(rateLimitService.isAccountBlocked("john@example.com")).thenReturn(true);

        assertThrows(LoginRateLimitException.class,
                () -> service.login(" JOHN@Example.com ", "password123"));
        org.mockito.Mockito.verifyNoInteractions(authenticationManager);
    }

    @Test
    void shouldThrowSpecificExceptionIfAuthenticatedUserCannotBeLoaded() {
        when(userRepository.findByEmailWithGroupsAndPermissions("john@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.login("john@example.com", "password123"));
        verify(rateLimitService).resetFailedLogins("john@example.com");
    }

    @Test
    void shouldRefreshWhenTokenIsValid() {
        var issued = new RefreshTokenPort.IssuedTokens("access", "refresh");
        when(refreshTokenPort.rotate("refresh-old")).thenReturn(Optional.of(issued));
        when(accessTokenPort.getExpiration()).thenReturn(900L);

        AuthenticationResult response = service.refresh("refresh-old");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        when(refreshTokenPort.rotate("invalid")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.refresh("invalid"));
    }

    @Test
    void shouldLogoutAndBlacklistBearerAccessToken() {
        service.logout("refresh", "Bearer access-token");

        verify(refreshTokenPort).revoke("refresh");
        verify(accessTokenPort).blacklistAccessToken("access-token");
    }

    @Test
    void shouldLogoutWithoutCallingRevokeForBlankRefreshToken() {
        service.logout(" ", "access-token");

        verify(accessTokenPort).blacklistAccessToken("access-token");
    }

    @Test
    void shouldExposeAuthenticationResultAccessors() {
        AuthenticationResult result = new AuthenticationResult("access", "refresh", 900L);

        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals(900L, result.expiresIn());
        assertTrue(result.expiresIn() > 0);
    }
}
