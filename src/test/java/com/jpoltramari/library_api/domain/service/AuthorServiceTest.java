package com.jpoltramari.library_api.domain.service;

import com.jpoltramari.library_api.api.dto.author.AuthorInput;
import com.jpoltramari.library_api.api.dto.author.AuthorUpdateInput;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.domain.exception.AuthorNotFoundException;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock private AuthorRepository repository;
    @Mock private AuthorMapper mapper;
    private AuthorService service;

    @BeforeEach
    void setUp() { service = new AuthorService(repository, mapper); }

    @Test
    void shouldFindAuthorOrFail() {
        Author author = new Author();
        when(repository.findById(1L)).thenReturn(Optional.of(author));
        assertEquals(author, service.findOrFail(1L));
    }

    @Test
    void shouldThrowWhenAuthorDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AuthorNotFoundException.class, () -> service.findOrFail(1L));
    }

    @Test
    void shouldCreateAuthor() {
        AuthorInput input = new AuthorInput("Robert Martin");
        Author author = new Author();
        when(mapper.toEntity(input)).thenReturn(author);
        when(repository.save(author)).thenReturn(author);
        assertEquals(author, service.create(input));
        verify(repository).save(author);
    }

    @Test
    void shouldUpdateAuthor() {
        Author author = new Author();
        AuthorUpdateInput input = new AuthorUpdateInput("Robert C. Martin");
        when(repository.findById(1L)).thenReturn(Optional.of(author));
        when(repository.save(author)).thenReturn(author);
        assertEquals(author, service.update(1L, input));
        verify(mapper).update(input, author);
    }

    @Test
    void shouldDeleteAuthor() {
        Author author = new Author();
        when(repository.findById(1L)).thenReturn(Optional.of(author));
        service.delete(1L);
        verify(repository).delete(author);
    }

    @Test
    void shouldListAuthors() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(new Author()), pageable, 1));
        assertEquals(1, service.findAll(pageable).getTotalElements());
    }
}
