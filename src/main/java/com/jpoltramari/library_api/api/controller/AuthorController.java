package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.dto.model.AuthorModel;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.domain.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService service;
    private final AuthorMapper mapper;

    public AuthorController(AuthorService service,
                            AuthorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<AuthorModel> list(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toModel);
    }

    @GetMapping("/{id}")
    public AuthorModel get(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    public ResponseEntity<AuthorModel> create(
            @RequestBody @Valid AuthorInput input) {

        AuthorModel model = mapper.toModel(service.save(input));

        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    public AuthorModel update(
            @PathVariable Long id,
            @RequestBody @Valid AuthorInput input) {

        return mapper.toModel(service.update(id, input));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}