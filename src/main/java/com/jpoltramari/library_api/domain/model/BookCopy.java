package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "book_copies")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(unique = true, nullable = false)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CopyStatus status;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private boolean active = true;
}
