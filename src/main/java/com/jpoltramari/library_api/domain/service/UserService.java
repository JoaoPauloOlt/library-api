package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<User> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public User findById(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public User save(User user){
        try{
            if (repository.existsByEmail(user.getEmail())){
                throw new BusinessException("Email already registered");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return repository.save(user);
        }catch (DataIntegrityViolationException e){
            throw new BusinessException("Email already registered");
        }
    }
}