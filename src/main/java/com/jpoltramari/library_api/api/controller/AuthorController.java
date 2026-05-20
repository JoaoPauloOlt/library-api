package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.dto.model.AuthorModel;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.domain.service.AuthorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
@Validated
public class AuthorController {

    private final AuthorService service;
    private final AuthorMapper mapper;

    @GetMapping
    public Page<AuthorModel> list(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toModel);
    }

    @GetMapping("/{id}")
    public AuthorModel findById(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorModel create(@RequestBody @Valid AuthorInput input) {
        return mapper.toModel(service.save(input));
    }

    @PutMapping("/{id}")
    public AuthorModel update(
            @PathVariable @Positive Long id,
            @RequestBody @Valid AuthorInput input) {

        return mapper.toModel(service.update(id, input));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}