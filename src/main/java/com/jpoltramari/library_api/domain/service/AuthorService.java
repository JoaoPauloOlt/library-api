package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.domain.exception.AuthorNotFoundException;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorService {

    private final AuthorRepository repository;
    private final AuthorMapper mapper;

    public AuthorService(AuthorRepository repository,
                         AuthorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<Author> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Author findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Transactional
    public Author save(AuthorInput input) {
        Author author = mapper.toEntity(input);
        return repository.save(author);
    }

    @Transactional
    public Author update(Long id, AuthorInput input) {
        Author author = findOrFail(id);
        mapper.update(input, author);
        return repository.save(author);
    }

    @Transactional
    public void delete(Long id) {
        Author author = findOrFail(id);
        repository.delete(author);
    }
}