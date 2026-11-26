package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.exception.LoginRateLimitException;
import com.jpoltramari.library_api.application.port.security.AccessTokenPort;
import com.jpoltramari.library_api.application.port.security.RefreshTokenPort;
import com.jpoltramari.library_api.application.result.auth.AuthenticationResult;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.PublicRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long ACCOUNT_RETRY_AFTER_SECONDS = 900;

    private final AuthenticationManager authenticationManager;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final UserRepository userRepository;
    private final PublicRateLimitService rateLimitService;

    @Transactional
    public AuthenticationResult login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (rateLimitService.isAccountBlocked(normalizedEmail)) {
            throw new LoginRateLimitException(ACCOUNT_RETRY_AFTER_SECONDS);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );
        } catch (AuthenticationException ex) {
            rateLimitService.recordFailedLogin(normalizedEmail);
            throw ex;
        }

        rateLimitService.resetFailedLogins(normalizedEmail);
        var user = userRepository.findByEmailWithGroupsAndPermissions(normalizedEmail)
                .orElseThrow();

        var issued = refreshTokenPort.issueTokens(user);
        return toAuthenticationResult(issued);
    }

    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        return refreshTokenPort.rotate(rawRefreshToken)
                .map(this::toAuthenticationResult)
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token"));
    }

    @Transactional
    public void logout(String rawRefreshToken, String accessToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenPort.revoke(rawRefreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            String token = accessToken.startsWith("Bearer ")
                    ? accessToken.substring(7).trim()
                    : accessToken.trim();
            accessTokenPort.blacklistAccessToken(token);
        }
    }

    private AuthenticationResult toAuthenticationResult(RefreshTokenPort.IssuedTokens issued) {
        return new AuthenticationResult(
                issued.accessToken(),
                issued.refreshToken(),
                accessTokenPort.getExpiration()
        );
    }
}
