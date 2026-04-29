package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.domain.exception.EntityInUseException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository repository;

    public Page<Author> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public Author findOrFail(Long id){
        return repository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Author not found with id " + id));
    }

    public Author save(Author input){
        Author author = new Author();
        BeanUtils.copyProperties(input, author);

        return repository.save(author);
    }

    public Author update(Long id, AuthorInput input){
        Author author = findOrFail(id);

        BeanUtils.copyProperties(input, author, "id");

        return repository.save(author);
    }

    public void delete(Long id){
        try {
            repository.deleteById(id);

        } catch (EmptyResultDataAccessException e){
            throw new EntityNotFoundException("Author not found with id " + id);

        } catch (DataIntegrityViolationException e){
            throw new EntityInUseException("Author with id " + id + " is in use");
        }
    }
}