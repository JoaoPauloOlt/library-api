package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.infrastructure.security.UserDetailsImpl;
import com.jpoltramari.library_api.domain.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Validated
public class LoanController {

    private final LoanService service;
    private final LoanMapper mapper;

    @GetMapping
    public Page<LoanModel> list(Pageable pageable) {
        return service.findAll(pageable).map(mapper::toModel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanModel create(@RequestBody @Valid LoanInput input,
                            @AuthenticationPrincipal UserDetailsImpl principal) {
        return mapper.toModel(service.create(input, principal.getUser()));
    }


    @PutMapping("/{id}/approve")
    public LoanModel approve(@PathVariable Long id) {
        return mapper.toModel(service.approve(id));
    }

    @PutMapping("/{id}/withdraw")
    public LoanModel withdraw(@PathVariable Long id) {
        return mapper.toModel(service.withdraw(id));
    }

    @PutMapping("/{id}/return")
    public LoanModel returnBook(@PathVariable Long id) {
        return mapper.toModel(service.returnBook(id));
    }

    @GetMapping("/{id}/cancel")
    public Page<LoanModel> cancel(Pageable pageable,
                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return service.findByUser(principal.getUser(), pageable).map(mapper::toModel);
    }

}