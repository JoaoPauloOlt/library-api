package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.assembler.LoanAssembler;
import com.jpoltramari.library_api.api.dto.input.LoanInput;
import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    @Autowired
    private LoanService service;

    @Autowired
    private LoanAssembler assembler;

    @GetMapping
    public List<LoanModel> list(Pageable pageable){
        return service.findAll(pageable)
                .stream()
                .map(assembler::toModel)
                .toList();
    }

    @PostMapping
    public ResponseEntity<LoanModel> create(@RequestBody @Valid LoanInput input){
        Loan loan = service.create(input);
        LoanModel model = assembler.toModel(loan);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(model.getId())
                .toUri();

        return ResponseEntity.created(uri).body(model);
    }

    @PutMapping("/{id}/return")
    public LoanModel returnBook(@PathVariable Long id){
        return assembler.toModel(service.returnBook(id));
    }
}