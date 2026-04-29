package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.domain.exception.BookNotFound;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityInUseException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.spec.BookSpecs;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    @Autowired
    private BookRepository repository;

    @Autowired
    private AuthorRepository authorRepository;

    public Page<Book> findAll(BookFilter filter, Pageable pageable){

        if (filter == null){
            filter = new BookFilter();
        }
        return repository.findAll(BookSpecs.usingFilter(filter), pageable);
    }

    public Book findByIsbnOrFail(String isbn){
        return repository.findByIsbn(isbn)
                .orElseThrow(()->
                        new BookNotFound("Book not found with ISBN " + isbn));
    }

    public Book findOrFail(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new BookNotFound("Book not found with id " + id));
    }

    public Book save(BookInput input){

        if (repository.existsByIsbn(input.getIsbn())){
            throw new BusinessException("ISBN already registered");
        }

        Author author = authorRepository.findById(input.getAuthorId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Author not found with id " + input.getAuthorId()));

        Book book = new Book();
        book.setTitle(input.getTitle());
        book.setIsbn(input.getIsbn());
        book.setGenre(input.getGenre());
        book.setAuthor(author);
        book.setAvailable(true);

        return repository.save(book);
    }

    public Book update(Long id, BookInput input){

        Book book = findOrFail(id);

        Author author = authorRepository.findById(input.getAuthorId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Author not found with id " + input.getAuthorId()));

        repository.findByIsbn(input.getIsbn())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(b -> {
                    throw new BusinessException("ISBN already registered");
                });

        book.setTitle(input.getTitle());
        book.setIsbn(input.getIsbn());
        book.setGenre(input.getGenre());
        book.setAuthor(author);

        return repository.save(book);
    }

    public void delete(Long id){
        Book book = findOrFail(id);
        try {
            repository.delete(book);

        } catch (DataIntegrityViolationException e){
            throw new EntityInUseException("Book with id " + id + "is in use");

        }
    }
}