package com.jpoltramari.library_api.infrastructure.dev;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import com.jpoltramari.library_api.infrastructure.security.jwt.RbacTokenInvalidationInitializer;
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

    @Autowired
    private RbacTokenInvalidationInitializer rbacTokenInvalidationInitializer;

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
                .contains("USER_ADMIN", "BOOK_DELETE", "LOAN_RETURN", "LOAN_CANCEL_ANY")
                .doesNotContain("LOAN_WITHDRAW");

        assertThat(librarianPermissions)
                .contains("BOOK_CREATE", "BOOK_COPY_UPDATE", "LOAN_APPROVE", "LOAN_RETURN", "LOAN_CANCEL_ANY")
                .doesNotContain("USER_ADMIN", "LOAN_WITHDRAW", "LOAN_CANCEL");

        assertThat(userPermissions)
                .containsExactlyInAnyOrder("BOOK_READ", "AUTHOR_READ", "LOAN_CREATE", "LOAN_CANCEL", "FINE_READ_OWN");
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
                """
                INSERT INTO group_permissions (group_id, permission_id)
                SELECT g.id, p.id
                FROM groups g
                CROSS JOIN permissions p
                WHERE g.name = 'LIBRARIAN' AND p.name = 'LOAN_CANCEL'
                ON CONFLICT DO NOTHING
                """
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
                """
                SELECT COUNT(*)
                FROM group_permissions gp
                JOIN groups g ON g.id = gp.group_id
                JOIN permissions p ON p.id = gp.permission_id
                WHERE g.name = 'LIBRARIAN' AND p.name = 'LOAN_CANCEL'
                """,
                Long.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM group_permissions gp
                JOIN groups g ON g.id = gp.group_id
                JOIN permissions p ON p.id = gp.permission_id
                WHERE g.name = 'LIBRARIAN' AND p.name = 'LOAN_CANCEL_ANY'
                """,
                Long.class
        )).isEqualTo(1L);

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
        rbacTokenInvalidationInitializer.initialize();

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
    @Transactional
    void shouldKeepRbacInvalidationTriggersActiveAfterRepeatedInitialization() {
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "user@library.com"
        );
        Long librarianGroupId = jdbcTemplate.queryForObject(
                "SELECT id FROM groups WHERE name = ?",
                Long.class,
                "LIBRARIAN"
        );
        Integer before = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                userId
        );

        rbacTokenInvalidationInitializer.initialize();

        jdbcTemplate.update(
                "INSERT INTO user_groups (user_id, group_id) VALUES (?, ?)",
                userId,
                librarianGroupId
        );

        Integer after = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        Integer installedTriggers = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE NOT tgisinternal
                  AND tgname IN (
                      'trg_user_groups_token_invalidation',
                      'trg_user_groups_token_invalidation_update',
                      'trg_group_permissions_token_invalidation',
                      'trg_group_permissions_token_invalidation_update',
                      'trg_groups_token_invalidation',
                      'trg_permissions_token_invalidation'
                  )
                """, Integer.class);

        assertThat(installedTriggers).isEqualTo(6);
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void shouldLoadDevLoanScenarios() {
        User user = userRepository.findByEmail("user@library.com").orElseThrow();

        assertThat(loanRepository.findByUserId(user.getId()))
                .extracting(loan -> loan.getStatus())
                .contains(LoanStatus.ACTIVE, LoanStatus.REQUESTED, LoanStatus.RETURNED, LoanStatus.LATE);
    }



    @Test
    @Transactional
    void shouldInvalidateTokensWhenUserGroupMembershipChanges() {
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "user@library.com"
        );
        Long groupId = jdbcTemplate.queryForObject(
                "SELECT id FROM groups WHERE name = ?",
                Long.class,
                "LIBRARIAN"
        );
        int before = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                userId
        );

        jdbcTemplate.update(
                "INSERT INTO user_groups (user_id, group_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                userId,
                groupId
        );
        int afterInsert = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        assertThat(afterInsert).isEqualTo(before + 1);

        jdbcTemplate.update(
                "DELETE FROM user_groups WHERE user_id = ? AND group_id = ?",
                userId,
                groupId
        );
        int afterDelete = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        assertThat(afterDelete).isEqualTo(afterInsert + 1);
    }

    @Test
    @Transactional
    void shouldInvalidateTokensWhenGroupPermissionsChange() {
        Long librarianId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "librarian@library.com"
        );
        Long permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM permissions WHERE name = ?",
                Long.class,
                "FINE_WAIVE"
        );
        Long groupId = jdbcTemplate.queryForObject(
                "SELECT id FROM groups WHERE name = ?",
                Long.class,
                "LIBRARIAN"
        );
        int before = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );

        jdbcTemplate.update(
                "INSERT INTO group_permissions (group_id, permission_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                groupId,
                permissionId
        );
        int afterInsert = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );
        assertThat(afterInsert).isEqualTo(before + 1);

        jdbcTemplate.update(
                "DELETE FROM group_permissions WHERE group_id = ? AND permission_id = ?",
                groupId,
                permissionId
        );
        int afterDelete = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );
        assertThat(afterDelete).isEqualTo(afterInsert + 1);
    }

    @Test
    @Transactional
    void shouldInvalidateTokensWhenGroupOrPermissionNamesChange() {
        Long librarianId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "librarian@library.com"
        );
        int beforeGroupRename = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );

        jdbcTemplate.update("UPDATE groups SET name = ? WHERE name = ?", "LIBRARIAN_TEMP", "LIBRARIAN");
        int afterGroupRename = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );
        assertThat(afterGroupRename).isEqualTo(beforeGroupRename + 1);

        jdbcTemplate.update("UPDATE groups SET name = ? WHERE name = ?", "LIBRARIAN", "LIBRARIAN_TEMP");
        Long permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM permissions WHERE name = ?",
                Long.class,
                "LOAN_APPROVE"
        );
        int beforePermissionRename = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );

        jdbcTemplate.update("UPDATE permissions SET name = ? WHERE id = ?", "LOAN_APPROVE_TEMP", permissionId);
        int afterPermissionRename = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?",
                Integer.class,
                librarianId
        );
        assertThat(afterPermissionRename).isEqualTo(beforePermissionRename + 1);
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
