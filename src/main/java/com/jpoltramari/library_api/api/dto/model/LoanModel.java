package com.jpoltramari.library_api.api.dto.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoanModel {

    private Long id;

    private String status;

    private String bookTitle;
    private String userName;

    private LocalDateTime requestDate;
    private LocalDateTime approvalDate;
    private LocalDateTime withdrawDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

}