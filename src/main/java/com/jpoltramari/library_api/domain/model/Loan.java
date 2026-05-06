package com.jpoltramari.library_api.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Entity
public class Loan {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    private LocalDateTime approvalDate;

    private LocalDateTime withdrawableDate;

    private LocalDateTime dueDate;

    private LocalDateTime returnDate;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private User user;
}
