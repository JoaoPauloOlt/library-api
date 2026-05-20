package com.jpoltramari.library_api.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.jpoltramari.library_api.domain.enums.Genre;

@Entity
@Table(name = "books")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();
}