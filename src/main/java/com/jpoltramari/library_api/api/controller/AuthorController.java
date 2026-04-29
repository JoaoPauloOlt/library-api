package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.assembler.AuthorAssembler;
import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.dto.model.AuthorModel;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    @Autowired
    private AuthorService service;

    @Autowired
    private AuthorAssembler assembler;

    @GetMapping
    public List<AuthorModel> list(Pageable pageable){
        return service.findAll(pageable)
                .stream()
                .map(assembler::toModel)
                .toList();
    }

    @GetMapping("/{id}")
    public AuthorModel search(@PathVariable Long id){
        return assembler.toModel(service.findOrFail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorModel add(@RequestBody @Valid AuthorInput input){
        Author author = assembler.toEntity(input);
        return assembler.toModel(service.save(author));
    }

    @PutMapping("/{id}")
    public AuthorModel update(@PathVariable Long id, @RequestBody @Valid AuthorInput input){
        Author currentAuthor = service.findOrFail(id);
        assembler.copyToEntity(input, currentAuthor);

        return assembler.toModel(service.save(currentAuthor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id){
        service.delete(id);
    }
}