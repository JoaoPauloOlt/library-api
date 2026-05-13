package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Entity
@Table(name = "loans")
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

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;
}