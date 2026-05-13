package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.api.dto.model.BookModel;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;
    private final BookMapper mapper;

    public BookController(BookService service, BookMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<BookModel> list(BookFilter filter, Pageable pageable) {
        return service.findAll(filter, pageable).map(mapper::toModel);
    }

    @GetMapping("/{id}")
    public BookModel get(@PathVariable Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    public BookModel create(@RequestBody @Valid BookInput input) {
        return mapper.toModel(service.save(input));
    }

    @PutMapping("/{id}")
    public BookModel update(@PathVariable Long id,
                            @RequestBody @Valid BookInput input) {
        return mapper.toModel(service.update(id, input));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}