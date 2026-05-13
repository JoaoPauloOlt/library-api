package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.LoginInput;
import com.jpoltramari.library_api.api.dto.model.LoginResponse;
import com.jpoltramari.library_api.domain.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginInput input){

        return ResponseEntity.ok(
                service.login(input.getEmail(), input.getPassword())
        );
    }
}