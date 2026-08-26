package com.jpoltramari.library_api.api.dto.loan;

import java.time.LocalDateTime;
import java.util.List;

public record LoanModel(

        Long id,
        String status,

        String bookTitle,
        String bookCoverUrl,
        List<String> bookAuthors,
        String userName,

        LocalDateTime requestDate,
        LocalDateTime approvalDate,
        LocalDateTime withdrawDate,
        LocalDateTime dueDate,
        LocalDateTime returnDate
) {}
