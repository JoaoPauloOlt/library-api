package com.jpoltramari.library_api.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@EqualsAndHashCode
@Getter
@Setter
@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime loanDate;

    private LocalDateTime returnDate;

    @ManyToOne
    private Book book;

    @ManyToOne
    private User user;
}
