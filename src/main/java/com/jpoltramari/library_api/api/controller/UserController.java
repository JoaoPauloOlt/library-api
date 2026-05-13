package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.dto.model.UserModel;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    public UserController(UserService service, UserMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<UserModel> list(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toModel);
    }

    @GetMapping("/{id}")
    public UserModel get(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    public UserModel create(@RequestBody @Valid UserInput input) {
        return mapper.toModel(service.save(input));
    }
}