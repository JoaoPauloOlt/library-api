package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.book.BookModel;
import com.jpoltramari.library_api.api.dto.book.BookUpdateInput;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.application.command.book.CreateBookCommand;
import com.jpoltramari.library_api.application.command.book.UpdateBookCommand;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.application.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "Books", description = "Book catalog management")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService service;
    private final BookMapper mapper;

    @GetMapping
    public PageResponse<BookModel> list(
            @ParameterObject @Valid BookFilter filter,
            @Parameter(description = "Pagination and sorting. Example: page=0&size=20&sort=title,asc")
            @PageableDefault(size = 20, sort = "title") Pageable pageable
    ) {
        return PageResponse.from(service.findAll(filter, pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    public BookModel findById(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookModel create(@RequestBody @Valid BookInput input) {
        CreateBookCommand command = new CreateBookCommand(
                input.isbn(), input.title(), input.genre(), input.description(),
                input.coverUrl(), input.quantity(), input.authorIds()
        );
        return mapper.toModel(service.create(command));
    }

    @PutMapping("/{id}")
    public BookModel update(
            @Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long id,
            @RequestBody @Valid BookUpdateInput input
    ) {
        UpdateBookCommand command = new UpdateBookCommand(
                input.isbn(), input.title(), input.genre(), input.description(),
                input.coverUrl(), input.authorIds()
        );
        return mapper.toModel(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long id) {
        service.delete(id);
    }
}
