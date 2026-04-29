package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public String login(String email, String password){

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new BusinessException("Invalid email or password");
        }

        return jwtService.generateToken(user);
    }
}