package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.infrastructure.security.AuthenticatedUser;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Validated
@Tag(name = "Loans", description = "Operations for loan requests and their lifecycle.")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService service;
    private final LoanMapper mapper;

    @GetMapping
    @Operation(summary = "List all loans", description = "Returns a paginated list of loans. The default ordering uses the request date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loans returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<LoanModel> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/my")
    @Operation(summary = "List my loans", description = "Returns the authenticated user's loans in paginated form.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loans returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<LoanModel> myLoans(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return PageResponse.from(
                service.findByUserId(principal.getUserId(), pageable),
                mapper::toModel
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel findById(@Parameter(description = "Loan identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a loan request", description = "Creates a loan request for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or business validation failure", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced book or resource not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel create(@RequestBody @Valid LoanInput input, @AuthenticationPrincipal AuthenticatedUser principal) {
        return mapper.toModel(service.create(input, principal.getUserId()));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a loan", description = "Approves a pending loan and transitions it to the active state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan ID or invalid lifecycle transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel approve(@Parameter(description = "Loan identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.approve(id));
    }

    @PutMapping("/{id}/return")
    @Operation(summary = "Return a loan", description = "Registers the return of an active loan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan ID or invalid lifecycle transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel returnBook(@Parameter(description = "Loan identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.returnBook(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a loan", description = "Cancels a loan according to the current lifecycle rules.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan ID or invalid lifecycle transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel cancel(@Parameter(description = "Loan identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.cancel(id));
    }
}
