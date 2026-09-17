package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.api.dto.user.UserModel;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.service.UserService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content)
    })
    public PageResponse<UserModel> list(
            @Parameter(in = ParameterIn.QUERY, name = "page", description = "Zero-based page number", example = "0") Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public UserModel get(@Parameter(description = "User identifier", example = "1") @PathVariable @Positive Long id) {
        return mapper.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user", description = "Creates a new user account. This endpoint is public and does not require a bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user data", content = @Content),
            @ApiResponse(responseCode = "409", description = "User email already registered", content = @Content)
    })
    public UserModel create(@RequestBody @Valid UserInput input) {
        return mapper.toModel(service.create(input));
    }
}
