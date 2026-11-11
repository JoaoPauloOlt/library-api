package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "book_copies",
        indexes = @Index(name = "idx_book_copies_book_id", columnList = "book_id")
)
@Check(constraints = "status IN ('AVAILABLE', 'LOANED', 'MAINTENANCE', 'LOST', 'DISCARDED')")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_book_copy_book")
    )
    private Book book;

    @Column(unique = true, nullable = false, length = 100)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CopyStatus status;

    @Column(length = 100)
    private String location;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean active = true;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
