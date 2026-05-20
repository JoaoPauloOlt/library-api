package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.domain.service.LoanService;
import com.jpoltramari.library_api.infrastructure.security.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Validated
public class LoanController {

    private final LoanService service;
    private final LoanMapper mapper;

    @GetMapping
    public Page<LoanModel> list(Pageable pageable) {
        return service.findAll(pageable)
                .map(mapper::toModel);
    }

    @GetMapping("/my")
    public Page<LoanModel> myLoans(
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return service.findByUser(principal.getUser(), pageable)
                .map(mapper::toModel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanModel create(
            @RequestBody @Valid LoanInput input,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return mapper.toModel(
                service.create(input, principal.getUser())
        );
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