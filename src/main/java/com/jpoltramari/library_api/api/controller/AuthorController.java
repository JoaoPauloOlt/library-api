package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.author.AuthorInput;
import com.jpoltramari.library_api.api.dto.author.AuthorModel;
import com.jpoltramari.library_api.api.dto.author.AuthorUpdateInput;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.domain.service.AuthorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    public PageResponse<AuthorModel> list(Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    public AuthorModel findById(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorModel create(@RequestBody @Valid AuthorInput input) {
        return mapper.toModel(service.create(input));
    }

    @PutMapping("/{id}")
    public AuthorModel update(
            @PathVariable @Positive Long id,
            @RequestBody @Valid AuthorUpdateInput input
    ) {
        return mapper.toModel(service.update(id, input));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}
