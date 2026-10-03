package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.bookcopy.BookAvailabilityModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyUpdateInput;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.api.mapper.BookCopyMapper;
import com.jpoltramari.library_api.application.command.bookcopy.CreateBookCopyCommand;
import com.jpoltramari.library_api.application.command.bookcopy.UpdateBookCopyCommand;
import com.jpoltramari.library_api.application.service.BookCopyService;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Book Copies", description = "Operations for managing physical copies of books.")
@SecurityRequirement(name = "bearerAuth")
public class BookCopyController {

    private final BookCopyService service;
    private final BookCopyMapper mapper;

    @GetMapping("/{id}")
    @Operation(summary = "Get a physical copy by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copy returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book or copy ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book copy not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BookCopyModel findById(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
                                  @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFailForBook(bookId, id));
    }

    @GetMapping
    @Operation(summary = "List physical copies of a book")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copies returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<BookCopyModel> findAllByBook(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId) {
        return service.findAllByBook(bookId).stream().map(mapper::toModel).toList();
    }

    @GetMapping("/availability")
    @Operation(summary = "Get book copy availability", description = "Returns total and currently available physical copies for a book.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BookAvailabilityModel availability(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId) {
        return new BookAvailabilityModel(bookId, service.totalQuantity(bookId), service.availableQuantity(bookId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a physical book copy")
    public BookCopyModel create(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
                                @RequestBody @Valid BookCopyInput input) {
        CreateBookCopyCommand command = new CreateBookCopyCommand(input.bookId(), input.location());
        return mapper.toModel(service.create(bookId, command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a physical book copy")
    public BookCopyModel update(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
                                @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id,
                                @RequestBody @Valid BookCopyUpdateInput input) {
        UpdateBookCopyCommand command = new UpdateBookCopyCommand(input.status(), input.location(), input.active());
        return mapper.toModel(service.update(bookId, id, command));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change physical copy status")
    public BookCopyModel changeStatus(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
                                      @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id,
                                      @Parameter(in = ParameterIn.QUERY, description = "New physical copy status", example = "AVAILABLE") @RequestParam CopyStatus status) {
        return mapper.toModel(service.changeStatus(bookId, id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a physical book copy")
    public void delete(@Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
                       @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id) {
        service.delete(bookId, id);
    }
}
