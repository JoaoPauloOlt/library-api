package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.exception.ErrorResponse;
import com.jpoltramari.library_api.application.command.user.CreateUserCommand;
import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.api.dto.user.UserModel;
import com.jpoltramari.library_api.api.dto.user.RegistrationResponse;
import com.jpoltramari.library_api.domain.exception.EmailAlreadyRegisteredException;
import com.jpoltramari.library_api.api.mapper.UserMapper;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "Operations for user administration and registration.")
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping
    @Operation(summary = "List users", description = "Returns a paginated list of users. This endpoint is restricted to users with administrative permission.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<UserModel> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserModel get(@Parameter(description = "User identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @Operation(summary = "Register a user", description = "Creates a new user account. The response is intentionally generic and does not disclose whether the email is already registered.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Registration request accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid user data", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RegistrationResponse> create(@RequestBody @Valid UserInput input) {
        RegistrationResponse response = RegistrationResponse.genericConfirmation();
        try {
            service.create(new CreateUserCommand(
                    input.name(), input.email(), input.telephone(), input.password()));
        } catch (EmailAlreadyRegisteredException ex) {
            // Keep the status and response body identical for existing and new email addresses.
        } catch (DataIntegrityViolationException ex) {
            if (!isEmailUniquenessViolation(ex)) {
                throw ex;
            }
            // Handles the concurrent duplicate-email race without revealing account existence.
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private boolean isEmailUniquenessViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(java.util.Locale.ROOT);
                if (normalized.contains("email")
                        && (normalized.contains("unique")
                        || normalized.contains("duplicate")
                        || normalized.contains("users_email"))) {
                    return true;
                }
            }
            if (current instanceof java.sql.SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                String sqlMessage = sqlException.getMessage();
                return sqlMessage != null
                        && sqlMessage.toLowerCase(java.util.Locale.ROOT).contains("email");
            }
            current = current.getCause();
        }
        return false;
    }
}