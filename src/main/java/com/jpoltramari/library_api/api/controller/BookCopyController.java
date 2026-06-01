package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.bookcopy.BookAvailabilityModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyUpdateInput;
import com.jpoltramari.library_api.api.mapper.BookCopyMapper;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.service.BookCopyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books/{bookId}/copies")
@RequiredArgsConstructor
@Validated
public class BookCopyController {

    private final BookCopyService service;
    private final BookCopyMapper mapper;

    @GetMapping("/{id}")
    public BookCopyModel findById(@PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @GetMapping
    public List<BookCopyModel> findAllByBook(@PathVariable @Positive Long bookId) {
        return service.findAllByBook(bookId)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @GetMapping("/availability")
    public BookAvailabilityModel availability(@PathVariable @Positive Long bookId) {
        return new BookAvailabilityModel(
                bookId,
                service.totalQuantity(bookId),
                service.availableQuantity(bookId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookCopyModel create(@RequestBody @Valid BookCopyInput input) {
        return mapper.toModel(service.create(input));
    }

    @PutMapping("/{id}")
    public BookCopyModel update(@PathVariable @Positive Long id, @RequestBody @Valid BookCopyUpdateInput input) {
        return mapper.toModel(service.update(id, input));
    }

    @PatchMapping("/{id}/status")
    public BookCopyModel changeStatus(@PathVariable @Positive Long id, @RequestParam CopyStatus status) {
        return mapper.toModel(service.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}