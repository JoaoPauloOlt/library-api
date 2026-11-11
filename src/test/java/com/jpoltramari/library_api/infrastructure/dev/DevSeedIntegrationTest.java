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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldLoadDevUsersWithExpectedGroups() {
        assertGroup("admin@library.com", "ADMIN");
        assertGroup("librarian@library.com", "LIBRARIAN");
        assertGroup("user@library.com", "USER");
    }

    @Test
    void shouldLoadSharedPermissionsForEachGroup() {
        Set<String> adminPermissions = permissionNames("admin@library.com");
        Set<String> librarianPermissions = permissionNames("librarian@library.com");
        Set<String> userPermissions = permissionNames("user@library.com");

        assertThat(adminPermissions)
                .contains("USER_ADMIN", "BOOK_DELETE", "LOAN_RETURN")
                .doesNotContain("LOAN_WITHDRAW");

        assertThat(librarianPermissions)
                .contains("BOOK_CREATE", "BOOK_COPY_UPDATE", "LOAN_APPROVE", "LOAN_RETURN")
                .doesNotContain("USER_ADMIN", "LOAN_WITHDRAW");

        assertThat(userPermissions)
                .containsExactlyInAnyOrder("BOOK_READ", "AUTHOR_READ", "LOAN_CREATE", "LOAN_CANCEL");
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
    void shouldLoadDevBookDescriptionsAndCovers() {
        assertBookHasDescriptionAndCover("9780451524935");
        assertBookHasDescriptionAndCover("9780141439518");
        assertBookHasDescriptionAndCover("9780743273565");
        assertBookHasDescriptionAndCover("9780316769488");
    }

    @Test
    @Transactional
    void shouldPreserveCustomDataWhenDevSeedsRunAgain() {
        jdbcTemplate.update(
                "UPDATE users SET name = ? WHERE email = ?",
                "Admin personalizado",
                "admin@library.com"
        );
        jdbcTemplate.update(
                "UPDATE books SET title = ?, cover_url = ? WHERE isbn = ?",
                "Título personalizado",
                "https://example.com/custom-cover.jpg",
                "9780451524935"
        );
        jdbcTemplate.update(
                "UPDATE groups SET description = ? WHERE name = ?",
                "Descrição personalizada",
                "ADMIN"
        );
        jdbcTemplate.update(
                "UPDATE book_copies SET status = 'MAINTENANCE' WHERE barcode = ?",
                "BC-1984-002"
        );
        jdbcTemplate.update(
                "INSERT INTO books (isbn, title, genre, description) VALUES (?, ?, ?, ?)",
                "9780000000001",
                "Livro criado durante o desenvolvimento",
                "DRAMA",
                "Este livro não deve receber exemplares automaticamente."
        );

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("data.sql"),
                new ClassPathResource("data-dev.sql")
        );
        DatabasePopulatorUtils.execute(populator, dataSource);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE email = ?",
                String.class,
                "admin@library.com"
        )).isEqualTo("Admin personalizado");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT title FROM books WHERE isbn = ?",
                String.class,
                "9780451524935"
        )).isEqualTo("Título personalizado");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT cover_url FROM books WHERE isbn = ?",
                String.class,
                "9780451524935"
        )).isEqualTo("https://example.com/custom-cover.jpg");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM groups WHERE name = ?",
                String.class,
                "ADMIN"
        )).isEqualTo("Descrição personalizada");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM book_copies WHERE barcode = ?",
                String.class,
                "BC-1984-002"
        )).isEqualTo("MAINTENANCE");

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM book_copies
                WHERE book_id = (SELECT id FROM books WHERE isbn = ?)
                """,
                Long.class,
                "9780000000001"
        )).isZero();
    }

    @Test
    @Transactional
    void shouldRestoreDevelopmentFixturesAfterDatabaseReset() {
        jdbcTemplate.update(
                "UPDATE users SET name = ? WHERE email = ?",
                "Admin personalizado",
                "admin@library.com"
        );
        jdbcTemplate.update(
                "INSERT INTO books (isbn, title, genre, description) VALUES (?, ?, ?, ?)",
                "9780000000001",
                "Livro temporário",
                "DRAMA",
                "Este registro deve ser removido pelo reset."
        );

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new FileSystemResource("scripts/reset-dev-db.sql"),
                new ClassPathResource("data.sql"),
                new ClassPathResource("data-dev.sql")
        );
        DatabasePopulatorUtils.execute(populator, dataSource);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE email = ?",
                String.class,
                "admin@library.com"
        )).isEqualTo("Admin");

        assertThat(bookRepository.findByIsbn("9780000000001")).isEmpty();
        assertThat(bookRepository.findByIsbn("9780451524935")).isPresent();
        assertThat(bookCopyRepository.findByBarcode("BC-1984-001")).isPresent();
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

    private Set<String> permissionNames(String email) {
        User user = userRepository.findByEmailWithGroupsAndPermissions(email).orElseThrow();

        return user.getGroups().stream()
                .flatMap(group -> group.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet());
    }

    private void assertBookHasDescriptionAndCover(String isbn) {
        var book = bookRepository.findByIsbn(isbn).orElseThrow();

        assertThat(book.getDescription()).isNotBlank();
        assertThat(book.getCoverUrl()).isNotBlank();
    }
}
