package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.exception.LoginRateLimitException;
import com.jpoltramari.library_api.application.port.security.AccessTokenPort;
import com.jpoltramari.library_api.application.port.security.RefreshTokenPort;
import com.jpoltramari.library_api.application.result.auth.AuthenticationResult;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.PublicRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpoltramari.library_api.domain.util.EmailNormalizer;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final UserRepository userRepository;
    private final PublicRateLimitService rateLimitService;

    @Transactional
    public AuthenticationResult login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        if (rateLimitService.isAccountBlocked(normalizedEmail)) {
            throw new LoginRateLimitException(rateLimitService.accountRetryAfterSeconds(normalizedEmail));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            rateLimitService.recordFailedLogin(normalizedEmail);
            throw ex;
        }

        rateLimitService.resetFailedLogins(normalizedEmail);
        var user = userRepository.findByEmailWithGroupsAndPermissions(normalizedEmail)
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user was not found."));

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
