package com.jpoltramari.library_api.infrastructure.dev;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DevSeedIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void shouldLoadDevUsersWithExpectedGroups() {
        assertGroup("admin@library.com", "ADMIN");
        assertGroup("librarian@library.com", "LIBRARIAN");
        assertGroup("user@library.com", "USER");
    }

    @Test
    void shouldLoadDevLibraryCatalogAndCopies() {
        assertThat(bookRepository.findByIsbn("9780451524935")).isPresent();
        assertThat(bookRepository.findByIsbn("9780141439518")).isPresent();
        assertThat(bookRepository.findByIsbn("9780743273565")).isPresent();
        assertThat(bookRepository.findByIsbn("9780316769488")).isPresent();

        assertThat(bookCopyRepository.findByBarcode("BC-1984-001")).isPresent();
        assertThat(bookCopyRepository.findByBarcode("BC-CATCHER-001")).isPresent();
    }

    @Test
    void shouldLoadDevLoanScenarios() {
        User user = userRepository.findByEmail("user@library.com").orElseThrow();

        assertThat(loanRepository.findByUserId(user.getId()))
                .extracting(loan -> loan.getStatus())
                .contains(LoanStatus.ACTIVE, LoanStatus.REQUESTED, LoanStatus.RETURNED);
    }

    private void assertGroup(String email, String expectedGroup) {
        User user = userRepository.findByEmailWithGroupsAndPermissions(email).orElseThrow();

        Set<String> groups = user.getGroups().stream()
                .map(group -> group.getName())
                .collect(Collectors.toSet());

        assertThat(groups).contains(expectedGroup);
    }
}
