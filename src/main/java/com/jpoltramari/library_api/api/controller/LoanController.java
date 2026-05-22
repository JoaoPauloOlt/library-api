package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.domain.service.LoanService;
import com.jpoltramari.library_api.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    public PageResponse<LoanModel> list(Pageable pageable) {
        return PageResponse.from(service.findAll(pageable), mapper::toModel);
    }

    @GetMapping("/my")
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
    public LoanModel create(
            @RequestBody @Valid LoanInput input,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return mapper.toModel(service.create(input, principal.getUserId()));
    }

    @PutMapping("/{id}/approve")
    public LoanModel approve(@PathVariable @Positive Long id) {
        return mapper.toModel(service.approve(id));
    }

    @PutMapping("/{id}/withdraw")
    public LoanModel withdraw(@PathVariable @Positive Long id) {
        return mapper.toModel(service.withdraw(id));
    }

    @PutMapping("/{id}/return")
    public LoanModel returnBook(@PathVariable @Positive Long id) {
        return mapper.toModel(service.returnBook(id));
    }

    @PutMapping("/{id}/cancel")
    public LoanModel cancel(@PathVariable @Positive Long id) {
        return mapper.toModel(service.cancel(id));
    }
}
