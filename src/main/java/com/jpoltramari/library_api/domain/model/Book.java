package com.jpoltramari.library_api.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import com.jpoltramari.library_api.domain.enums.Genre;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books")
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include private Long id;
    @Column(nullable = false, unique = true, length = 13) private String isbn;
    @Column(nullable = false, length = 150) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Genre genre;
    @Column(columnDefinition = "TEXT") private String description;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "cover_url", length = 500) private String coverUrl;
    @Formula("(select count(l.id) from loans l join book_copies bc on bc.id = l.book_copy_id where bc.book_id = id and l.status in ('ACTIVE', 'RETURNED', 'LATE'))") private Long loanCount;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_authors", joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY) private Set<BookCopy> copies = new HashSet<>();
}
