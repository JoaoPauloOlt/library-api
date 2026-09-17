package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.auth.LoginInput;
import com.jpoltramari.library_api.api.dto.auth.LoginResponse;
import com.jpoltramari.library_api.api.dto.auth.LogoutInput;
import com.jpoltramari.library_api.api.dto.auth.RefreshTokenInput;
import com.jpoltramari.library_api.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and JWT session management")
public class AuthController {

    private final AuthService service;

    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns access and refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials payload"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginInput input) {
        return service.login(input.email(), input.password());
    }

    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token payload"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody @Valid RefreshTokenInput input) {
        return service.refresh(input.refreshToken());
    }

    @Operation(summary = "Logout", description = "Invalidates the current access and refresh token session.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token payload"),
            @ApiResponse(responseCode = "401", description = "Invalid authentication")
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutInput input) {
        service.logout(input.refreshToken(), input.accessToken());
    }
}
