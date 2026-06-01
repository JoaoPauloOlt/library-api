package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.auth.LoginInput;
import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.api.dto.auth.LogoutInput;
import com.jpoltramari.library_api.api.dto.auth.RefreshTokenInput;
import com.jpoltramari.library_api.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginInput input) {
        return service.login(input.email(), input.password());
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody @Valid RefreshTokenInput input) {
        return service.refresh(input.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutInput input) {
        service.logout(input.refreshToken(), input.accessToken());
    }
}
