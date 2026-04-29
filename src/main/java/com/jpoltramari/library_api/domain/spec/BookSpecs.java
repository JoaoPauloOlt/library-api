package com.jpoltramari.library_api.domain.spec;

import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.filter.BookFilter;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;

import java.util.ArrayList;

public class BookSpecs {

    public static Specification<Book> usingFilter(BookFilter filter){

        return (root, query, builder) -> {

            var predicates = new ArrayList<>();

            if (filter.getTitle() != null){
                predicates.add(
                        builder.like(
                                builder.lower(root.get("title")),
                                "%" + filter.getTitle().toLowerCase() + "%"
                        )
                );
            }

            if (filter.getGenre() != null){
                predicates.add(
                        builder.equal(root.get("genre"), filter.getGenre())
                );
            }

            if (filter.getAvailable() != null){
                predicates.add(
                        builder.equal(root.get("available"), filter.getAvailable())
                );
            }

            if (filter.getAuthorName() != null){
                Join<Book, Author> authorJoin = root.join("author", JoinType.LEFT);

                predicates.add(
                        builder.like(
                                builder.lower(authorJoin.get("name")),
                                "%" + filter.getAuthorName().toLowerCase() + "%"
                        )
                );
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
