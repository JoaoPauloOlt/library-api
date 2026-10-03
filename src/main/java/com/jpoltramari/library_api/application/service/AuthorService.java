package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.author.CreateAuthorCommand;
import com.jpoltramari.library_api.application.command.author.UpdateAuthorCommand;
import com.jpoltramari.library_api.domain.exception.AuthorNotFoundException;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository repository;

    public Page<Author> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Author findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @Transactional
    public Author create(CreateAuthorCommand command) {
        Author author = new Author();
        author.setName(command.name());
        author.setNationality(command.nationality());
        return repository.save(author);
    }

    @Transactional
    public Author update(Long id, UpdateAuthorCommand command) {
        Author author = findOrFail(id);

        if (command.name() != null) {
            author.setName(command.name());
        }

        if (command.nationality() != null) {
            author.setNationality(command.nationality());
        }

        return repository.save(author);
    }

    @Transactional
    public void delete(Long id) {
        Author author = findOrFail(id);
        repository.delete(author);
    }
}