package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.infrastructure.security.SecurityExpressions;
import com.jpoltramari.library_api.domain.service.LoanService;
import com.jpoltramari.library_api.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Validated
public class LoanController {

    private final LoanService service;
    private final LoanMapper mapper;

    @GetMapping
    @PreAuthorize(SecurityExpressions.LOAN_MANAGE)
    public PageResponse<LoanModel> list(Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/my")
    @PreAuthorize(SecurityExpressions.CREATE_LOAN)
    public PageResponse<LoanModel> myLoans(
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return PageResponse.from(
                service.findByUserId(principal.getUserId(), pageable),
                mapper::toModel
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SecurityExpressions.CREATE_LOAN)
    public LoanModel create(
            @RequestBody @Valid LoanInput input,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return mapper.toModel(service.create(input, principal.getUserId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize(SecurityExpressions.APPROVE_LOAN)
    public LoanModel approve(@PathVariable @Positive Long id) {
        return mapper.toModel(service.approve(id));
    }

    @PutMapping("/{id}/withdraw")
    @PreAuthorize(SecurityExpressions.WITHDRAW_LOAN)
    public LoanModel withdraw(@PathVariable @Positive Long id) {
        return mapper.toModel(service.withdraw(id));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize(SecurityExpressions.RETURN_BOOK)
    public LoanModel returnBook(@PathVariable @Positive Long id) {
        return mapper.toModel(service.returnBook(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize(SecurityExpressions.CANCEL_LOAN)
    public LoanModel cancel(@PathVariable @Positive Long id) {
        return mapper.toModel(service.cancel(id));
    }
}
