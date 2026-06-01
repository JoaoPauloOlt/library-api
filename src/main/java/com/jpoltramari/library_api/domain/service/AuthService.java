package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.JwtService;
import com.jpoltramari.library_api.infrastructure.security.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Transactional
    public LoginResponse login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        var user = userRepository.findByEmailWithGroupsAndPermissions(email)
                .orElseThrow();

        var issued = refreshTokenService.issueTokens(user);
        return toLoginResponse(issued.accessToken(), issued.refreshToken());
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        return refreshTokenService.rotate(rawRefreshToken)
                .map(issued -> toLoginResponse(
                        issued.accessToken(),
                        issued.refreshToken()
                ))
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token"));
    }

    @Transactional
    public void logout(String rawRefreshToken, String accessToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            String token = accessToken.startsWith("Bearer ")
                    ? accessToken.substring(7).trim()
                    : accessToken.trim();
            jwtService.blacklistAccessToken(token);
        }
    }

    private LoginResponse toLoginResponse(String accessToken, String refreshToken) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtService.getExpiration()
        );
    }
}
