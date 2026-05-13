package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.model.LoginResponse;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.infrastructure.security.JwtService;
import com.jpoltramari.library_api.infrastructure.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String email, String password) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                password
                        )
                );

        User user = ((UserDetailsImpl) authentication.getPrincipal())
                .getUser();

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getName(),
                user.getEmail()
        );
    }
}