package com.jpoltramari.library_api.api.dto;

import com.jpoltramari.library_api.api.dto.author.AuthorInput;
import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyInput;
import com.jpoltramari.library_api.api.dto.loan.LoanInput;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.domain.enums.Genre;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationContractTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldRejectInvalidBookInput() {
        BookInput input = new BookInput(
                "invalid-isbn",
                "",
                null,
                "description",
                "not-a-url",
                -1,
                List.of()
        );

        var violations = validator.validate(input);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("isbn")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("genre")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("authorIds")));
    }

    @Test
    void shouldAcceptValidBookInput() {
        BookInput input = new BookInput(
                "9780451524935",
                "1984",
                Genre.DRAMA,
                null,
                "https://example.com/cover.jpg",
                3,
                List.of(1L)
        );

        assertTrue(validator.validate(input).isEmpty());
    }

    @Test
    void shouldRejectInvalidAuthorInput() {
        AuthorInput input = new AuthorInput(" ", "");

        var violations = validator.validate(input);

        assertEquals(2, violations.size());
    }

    @Test
    void shouldRejectInvalidBookCopyAndLoanIds() {
        BookCopyInput copy = new BookCopyInput(null, " ");
        LoanInput loan = new LoanInput(null);

        assertFalse(validator.validate(copy).isEmpty());
        assertFalse(validator.validate(loan).isEmpty());
    }

    @Test
    void shouldRejectInvalidUserInput() {
        UserInput input = new UserInput(" ", "invalid", "", "123");

        var violations = validator.validate(input);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("telephone")));
    }
}
