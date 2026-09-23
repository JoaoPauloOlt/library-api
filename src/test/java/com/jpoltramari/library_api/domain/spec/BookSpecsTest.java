package com.jpoltramari.library_api.domain.spec;

import com.jpoltramari.library_api.domain.filter.BookFilter;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookSpecsTest {

    @Test
    void shouldBuildPredicateForTitleAndGenre() {
        BookFilter filter = new BookFilter();
        filter.setTitle("Java");
        filter.setGenre("TECH");

        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Book> query = mock(CriteriaQuery.class);
        Root<Book> root = mock(Root.class);
        jakarta.persistence.criteria.Path<String> title = mock(jakarta.persistence.criteria.Path.class);
        jakarta.persistence.criteria.Path<String> genre = mock(jakarta.persistence.criteria.Path.class);
        Predicate titlePredicate = mock(Predicate.class);
        Predicate genrePredicate = mock(Predicate.class);

        doReturn(title).when(root).get("title");
        doReturn(genre).when(root).get("genre");
        when(builder.lower(title)).thenReturn(title);
        when(builder.like(title, "%java%")).thenReturn(titlePredicate);
        when(builder.equal(genre, "TECH")).thenReturn(genrePredicate);
        when(builder.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<Book> specification = BookSpecs.usingFilter(filter);
        specification.toPredicate(root, query, builder);

        verify(builder).like(title, "%java%");
        verify(builder).equal(genre, "TECH");
        verify(builder).and(any(Predicate[].class));
        verify(query, never()).distinct(true);
    }

    @Test
    void shouldJoinAuthorAndEnableDistinctWhenAuthorFilterIsPresent() {
        BookFilter filter = new BookFilter();
        filter.setAuthorName("Machado");

        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Book> query = mock(CriteriaQuery.class);
        Root<Book> root = mock(Root.class);
        Join<Book, Author> authorJoin = mock(Join.class);
        jakarta.persistence.criteria.Path<String> authorName = mock(jakarta.persistence.criteria.Path.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(authorJoin).when(root).join("authors", JoinType.LEFT);
        doReturn(authorName).when(authorJoin).get("name");
        when(builder.lower(authorName)).thenReturn(authorName);
        when(builder.like(authorName, "%machado%")).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        BookSpecs.usingFilter(filter).toPredicate(root, query, builder);

        verify(root).join("authors", JoinType.LEFT);
        verify(builder).like(authorName, "%machado%");
        verify(query).distinct(true);
    }

    @Test
    void shouldBuildEmptyPredicateWhenFilterHasNoValues() {
        BookFilter filter = new BookFilter();
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Book> query = mock(CriteriaQuery.class);
        Root<Book> root = mock(Root.class);

        when(builder.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        BookSpecs.usingFilter(filter).toPredicate(root, query, builder);

        verify(builder).and(any(Predicate[].class));
        verifyNoInteractions(root);
    }
}
