package com.jpoltramari.library_api.api.dto.loan;

import java.time.LocalDateTime;

public record LoanModel(

        Long id,
        String status,

        String bookTitle,
        String userName,

        LocalDateTime requestDate,
        LocalDateTime approvalDate,
        LocalDateTime withdrawDate,
        LocalDateTime dueDate,
        LocalDateTime returnDate
) {}
