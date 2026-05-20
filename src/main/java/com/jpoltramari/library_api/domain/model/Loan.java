package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    private LocalDateTime approvalDate;
    private LocalDateTime withdrawableDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;

    @Column(nullable = false)
    private Integer renewalCount = 0;
}