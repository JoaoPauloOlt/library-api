package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.api.dto.user.UserModel;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping
    public PageResponse<UserModel> list(Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    public UserModel get(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserModel create(@RequestBody @Valid UserInput input) {
        return mapper.toModel(service.create(input));
    }
}
