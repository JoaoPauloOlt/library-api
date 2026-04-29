package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.assembler.BookAssembler;
import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.api.dto.model.BookModel;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    private BookService service;

    @Autowired
    private BookAssembler assembler;

    @GetMapping
    public List<BookModel> list(@ModelAttribute BookFilter filter){
        return service.findAll(filter, Pageable.unpaged())
                .stream()
                .map(assembler::toModel)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookModel> find(@PathVariable Long id){
        Book book = service.findOrFail(id);
        return ResponseEntity.ok(assembler.toModel(book));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookModel> findByIsbn(@PathVariable String isbn){
        Book book = service.findByIsbnOrFail(isbn);
        return ResponseEntity.ok(assembler.toModel(book));
    }

    @PostMapping
    public ResponseEntity<BookModel> create(@RequestBody @Valid BookInput input){
        Book book = service.save(input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assembler.toModel(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookModel> update(@PathVariable Long id,
                                            @RequestBody @Valid BookInput input){

        Book book = service.update(id, input);
        return ResponseEntity.ok(assembler.toModel(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
