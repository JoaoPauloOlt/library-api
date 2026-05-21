package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.book.BookModel;
import com.jpoltramari.library_api.api.dto.book.BookUpdateInput;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.infrastructure.security.SecurityExpressions;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Validated
public class BookController {

    private final BookService service;
    private final BookMapper mapper;

    @GetMapping
    @PreAuthorize(SecurityExpressions.BOOK_READ)
    public PageResponse<BookModel> list(BookFilter filter, Pageable pageable) {
        return PageResponse.from(service.findAll(filter, pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityExpressions.BOOK_READ)
    public BookModel findById(@PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SecurityExpressions.BOOK_CREATE)
    public BookModel create(@RequestBody @Valid BookInput input) {
        return mapper.toModel(service.create(input));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityExpressions.BOOK_UPDATE)
    public BookModel update(
            @PathVariable @Positive Long id,
            @RequestBody @Valid BookUpdateInput input
    ) {
        return mapper.toModel(service.update(id, input));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(SecurityExpressions.BOOK_DELETE)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}
