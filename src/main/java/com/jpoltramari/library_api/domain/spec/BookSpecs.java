package com.jpoltramari.library_api.domain.spec;

import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;

import java.util.ArrayList;
import java.util.List;

public class BookSpecs {

    private BookSpecs(){

    }

    public static Specification<Book> usingFilter(BookFilter filter){

        return (root, query, builder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getTitle() != null && !filter.getTitle().isBlank()){
                predicates.add(
                        builder.like(
                                builder.lower(root.get("title")),
                                "%" + filter.getTitle().toLowerCase() + "%"
                        )
                );
            }

            if (filter.getGenre() != null && !filter.getGenre().isBlank()){
                predicates.add(
                        builder.equal(root.get("genre"), filter.getGenre())
                );
            }

            if (filter.getAuthorName() != null && !filter.getAuthorName().isBlank()){
                Join<Book, Author> authorJoin = root.join("authors", JoinType.LEFT);

                predicates.add(
                        builder.like(
                                builder.lower(authorJoin.get("name")),
                                "%" + filter.getAuthorName().toLowerCase() + "%"
                        )
                );
                query.distinct(true);
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
