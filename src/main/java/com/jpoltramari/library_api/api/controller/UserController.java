package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.assembler.UserAssembler;
import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.dto.model.UserModel;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private UserAssembler assembler;

    @GetMapping
    public List<UserModel> list(Pageable pageable){
        return service.findAll(pageable)
                .stream()
                .map(assembler::toModel)
                .toList();
    }

    @GetMapping("/{id}")
    public UserModel search(@PathVariable Long id){
        return assembler.toModel(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserModel add(@RequestBody @Valid UserInput input){
        User user = assembler.toEntity(input);
        return assembler.toModel(service.save(user));
    }
}