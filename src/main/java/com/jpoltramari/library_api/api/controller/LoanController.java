package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.loan.LoanForUserInput;
import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.dto.loan.LoanUserModel;
import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.application.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;
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
            Authentication authentication) {
        return PageResponse.from(
                service.findByUserId(authenticatedPrincipal(authentication).getUserId(), pageable),
                mapper::toModel
        );
    }

    @PreAuthorize("hasAuthority('LOAN_CREATE_FOR_USER')")
    @GetMapping("/users")
    @Operation(
            summary = "List active users eligible for staff circulation",
            description = "Returns a minimal, paginated list of active users. Requires LOAN_CREATE_FOR_USER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active users returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<LoanUserModel> listEligibleUsers(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return PageResponse.from(
                userService.findActiveBorrowers(pageable),
                user -> new LoanUserModel(user.getId(), user.getName(), user.getEmail())
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
    public LoanModel create(@RequestBody @Valid LoanInput input, Authentication authentication) {
        return mapper.toModel(service.create(
                new CreateLoanCommand(input.bookId()),
                authenticatedPrincipal(authentication).getUserId()
        ));
    }

    @PreAuthorize("hasAuthority('LOAN_CREATE_FOR_USER')")
    @PostMapping("/for-user")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a loan for another user",
            description = "Allows an authorized librarian or administrator to create a loan request for an active user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or inactive borrower", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced book or user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel createForUser(@RequestBody @Valid LoanForUserInput input) {
        return mapper.toModel(service.createForUser(
                new CreateLoanCommand(input.bookId()),
                input.userId()
        ));
    }

    private AuthenticatedPrincipal authenticatedPrincipal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
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
    @Operation(summary = "Cancel a loan", description = "Cancels the authenticated user's loan, or any loan when the caller has LOAN_CANCEL_ANY permission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan ID or invalid lifecycle transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public LoanModel cancel(
            @Parameter(description = "Loan identifier", example = "1") @PathVariable @Positive Long id,
            Authentication authentication) {
        return mapper.toModel(service.cancel(id, authenticatedPrincipal(authentication)));
    }
}
