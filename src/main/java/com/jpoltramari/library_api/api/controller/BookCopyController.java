package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.bookcopy.BookAvailabilityModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyModel;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyUpdateInput;
import com.jpoltramari.library_api.api.mapper.BookCopyMapper;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.service.BookCopyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
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
            @ApiResponse(responseCode = "400", description = "Invalid book or copy ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book copy not found", content = @Content)
    })
    public BookCopyModel findById(
            @Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
            @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @GetMapping
    @Operation(summary = "List physical copies of a book")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copies returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    public List<BookCopyModel> findAllByBook(
            @Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId) {
        return service.findAllByBook(bookId)
                .stream()
                .map(mapper::toModel)
                .toList();
    }

    @GetMapping("/availability")
    @Operation(summary = "Get book copy availability", description = "Returns total and currently available physical copies for a book.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    public BookAvailabilityModel availability(
            @Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId) {
        return new BookAvailabilityModel(
                bookId,
                service.totalQuantity(bookId),
                service.availableQuantity(bookId)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a physical book copy")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book copy created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book copy data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    public BookCopyModel create(@RequestBody @Valid BookCopyInput input) {
        return mapper.toModel(service.create(input));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a physical book copy")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copy updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid book copy data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book copy not found", content = @Content)
    })
    public BookCopyModel update(
            @Parameter(description = "Book identifier", example = "1") @PathVariable @Positive Long bookId,
            @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id,
            @RequestBody @Valid BookCopyUpdateInput input) {
        return mapper.toModel(service.update(id, input));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change physical copy status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copy status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid copy ID or status", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book copy not found", content = @Content)
    })
    public BookCopyModel changeStatus(
            @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id,
            @Parameter(in = ParameterIn.QUERY, description = "New physical copy status", example = "AVAILABLE") @RequestParam CopyStatus status) {
        return mapper.toModel(service.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a physical book copy")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book copy deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid copy ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book copy not found", content = @Content)
    })
    public void delete(
            @Parameter(description = "Physical copy identifier", example = "1") @PathVariable @Positive Long id) {
        service.delete(id);
    }
}