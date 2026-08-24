package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.JwtService;
import com.jpoltramari.library_api.infrastructure.security.jwt.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;

    private AuthService service;

    @BeforeEach
    void setUp() { service = new AuthService(authenticationManager, jwtService, refreshTokenService, userRepository); }

    @Test
    void shouldLoginAndIssueTokens() {
        User user = new User();
        var issued = new RefreshTokenService.IssuedTokens("access", "refresh");
        when(userRepository.findByEmailWithGroupsAndPermissions("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.issueTokens(user)).thenReturn(issued);
        when(jwtService.getExpiration()).thenReturn(900L);

        LoginResponse response = service.login("john@example.com", "password123");

        assertEquals("access", response.token());
        assertEquals("refresh", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldRefreshWhenTokenIsValid() {
        var issued = new RefreshTokenService.IssuedTokens("access", "refresh");
        when(refreshTokenService.rotate("refresh-old")).thenReturn(Optional.of(issued));
        when(jwtService.getExpiration()).thenReturn(900L);

        LoginResponse response = service.refresh("refresh-old");

        assertEquals("access", response.token());
        assertEquals("refresh", response.refreshToken());
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        when(refreshTokenService.rotate("invalid")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.refresh("invalid"));
    }

    @Test
    void shouldLogoutAndBlacklistBearerAccessToken() {
        service.logout("refresh", "Bearer access-token");

        verify(refreshTokenService).revoke("refresh");
        verify(jwtService).blacklistAccessToken("access-token");
    }

    @Test
    void shouldLogoutWithoutCallingRevokeForBlankRefreshToken() {
        service.logout(" ", "access-token");

        verify(jwtService).blacklistAccessToken("access-token");
    }
}
