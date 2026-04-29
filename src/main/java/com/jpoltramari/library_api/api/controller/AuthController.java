package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.LoginInput;
import com.jpoltramari.library_api.domain.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginInput input){
        String token = service.login(input.getEmail(), input.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }
}