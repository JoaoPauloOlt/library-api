package com.jpoltramari.library_api.api.dto.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoanModel {

    private Long id;
    private String bookTitle;
    private String userName;
    private LocalDateTime loanDate;
    private LocalDateTime returnDate;

}