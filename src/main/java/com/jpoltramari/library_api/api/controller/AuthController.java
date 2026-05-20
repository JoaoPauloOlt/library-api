package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.LoginInput;
import com.jpoltramari.library_api.api.dto.model.LoginResponse;
import com.jpoltramari.library_api.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginInput input){
        return AuthService.login(input.getEmail(), input.getPassword());
    }
}