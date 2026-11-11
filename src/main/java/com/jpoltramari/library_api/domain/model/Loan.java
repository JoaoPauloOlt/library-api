package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "loans",
        indexes = {
                @Index(name = "idx_loans_user_id", columnList = "user_id"),
                @Index(name = "idx_loans_book_copy_id", columnList = "book_copy_id")
        }
)
@Check(constraints = "status IN ('REQUESTED', 'ACTIVE', 'RETURNED', 'LATE', 'CANCELED')")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    private LocalDateTime approvalDate;
    private LocalDateTime withdrawableDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loans_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "book_copy_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loans_book_copy")
    )
    private BookCopy bookCopy;

    @Column(
            name = "renewal_count",
            nullable = false,
            columnDefinition = "INTEGER NOT NULL DEFAULT 0"
    )
    private Integer renewalCount = 0;
}
