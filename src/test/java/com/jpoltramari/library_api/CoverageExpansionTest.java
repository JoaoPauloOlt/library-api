package com.jpoltramari.library_api;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.auth.RefreshTokenInput;
import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.user.UserUpdateInput;
import com.jpoltramari.library_api.application.command.bookcopy.UpdateBookCopyCommand;
import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.UserSecuritySnapshotPort;
import com.jpoltramari.library_api.application.service.BookCopyService;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.application.service.TokenVersionService;
import com.jpoltramari.library_api.application.service.UserService;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.exception.LoanNotFoundException;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.Permission;
import com.jpoltramari.library_api.domain.model.RefreshToken;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.GroupRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.RefreshTokenRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoverageExpansionTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldBuildPaginatedResponsesWithAndWithoutMapping() {
        Page<Integer> page = new PageImpl<>(
                List.of(10, 20),
                PageRequest.of(0, 2),
                3
        );

        PageResponse<Integer> direct = PageResponse.from(page);
        assertEquals(List.of(10, 20), direct.content());
        assertEquals(0, direct.page());
        assertEquals(2, direct.size());
        assertEquals(3, direct.totalElements());
        assertEquals(2, direct.totalPages());
        assertTrue(direct.first());
        assertFalse(direct.last());

        PageResponse<String> mapped = PageResponse.from(page, String::valueOf);
        assertEquals(List.of("10", "20"), mapped.content());
        assertEquals(direct.totalElements(), mapped.totalElements());
    }

    @Test
    void shouldRevokeAllSessionsAcrossPersistenceAndSnapshotPorts() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        UserSecuritySnapshotPort snapshotPort = mock(UserSecuritySnapshotPort.class);
        TokenVersionService service = new TokenVersionService(
                userRepository,
                refreshTokenRepository,
                snapshotPort
        );

        service.revokeAllSessions(42L);

        verify(userRepository).incrementTokenVersion(42L);
        verify(refreshTokenRepository).revokeAllByUserId(org.mockito.ArgumentMatchers.eq(42L), any(Instant.class));
        verify(snapshotPort).evict(42L);
    }

    @Test
    void shouldExposeLoanQueriesAndReportMissingLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        LoanService service = new LoanService(
                loanRepository,
                bookRepository,
                bookCopyRepository,
                userRepository,
                FIXED_CLOCK
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Loan> expected = new PageImpl<>(List.of(new Loan()));
        when(loanRepository.findAll(pageable)).thenReturn(expected);
        when(loanRepository.findByUserId(42L, pageable)).thenReturn(expected);

        assertSame(expected, service.findAll(pageable));
        assertSame(expected, service.findByUserId(42L, pageable));

        Loan loan = new Loan();
        when(loanRepository.findById(7L)).thenReturn(Optional.of(loan));
        when(loanRepository.findById(8L)).thenReturn(Optional.empty());

        assertSame(loan, service.findOrFail(7L));
        assertThrows(LoanNotFoundException.class, () -> service.findOrFail(8L));
    }

    @Test
    void shouldRejectCopyThatBecameUnavailableBeforeLoanReservation() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        LoanService service = new LoanService(
                loanRepository,
                bookRepository,
                bookCopyRepository,
                userRepository,
                FIXED_CLOCK
        );

        User borrower = new User();
        borrower.setId(42L);
        borrower.setStatus(UserStatus.ACTIVE);

        BookCopy copy = new BookCopy();
        copy.setId(15L);
        copy.setStatus(CopyStatus.RESERVED);
        copy.setActive(true);

        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(borrower));
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(loanRepository.existsByUserIdAndBookCopyBookIdAndStatusIn(
                42L,
                10L,
                List.of(LoanStatus.REQUESTED, LoanStatus.ACTIVE, LoanStatus.LATE)
        )).thenReturn(false);
        when(bookCopyRepository.findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(
                10L,
                CopyStatus.AVAILABLE
        )).thenReturn(Optional.of(copy));

        assertThrows(
                BusinessException.class,
                () -> service.createForUser(new CreateLoanCommand(10L), 42L)
        );

        verify(bookCopyRepository, never()).save(any(BookCopy.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void shouldCoverBookCopyLookupAndRetirementTransitions() {
        BookRepository bookRepository = mock(BookRepository.class);
        BookCopyRepository repository = mock(BookCopyRepository.class);
        BookCopyService service = new BookCopyService(bookRepository, repository);

        BookCopy copy = new BookCopy();
        copy.setId(8L);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);

        when(repository.findById(8L)).thenReturn(Optional.of(copy));
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertSame(copy, service.findOrFail(8L));
        assertThrows(EntityNotFoundException.class, () -> service.findOrFail(9L));

        when(bookRepository.existsById(5L)).thenReturn(true);
        when(repository.findByIdAndBookIdForUpdate(8L, 5L)).thenReturn(Optional.of(copy));
        when(repository.save(copy)).thenReturn(copy);

        BookCopy updated = service.update(
                5L,
                8L,
                new UpdateBookCopyCommand(CopyStatus.LOST, null, null)
        );

        assertSame(copy, updated);
        assertEquals(CopyStatus.LOST, updated.getStatus());
        assertFalse(updated.isActive());

        when(bookRepository.existsById(99L)).thenReturn(false);
        assertThrows(BookNotFoundException.class, () -> service.findAllByBook(99L));
    }

    @Test
    void shouldExposeUserListQuery() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(
                repository,
                mock(GroupRepository.class),
                mock(PasswordEncoder.class)
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> expected = new PageImpl<>(List.of(new User()));
        when(repository.findAll(pageable)).thenReturn(expected);

        assertSame(expected, service.findAll(pageable));
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldPreserveCauseInBusinessException() {
        IllegalStateException cause = new IllegalStateException("database constraint");

        BusinessException exception = new BusinessException("Operation failed", cause);

        assertEquals("Operation failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldSupportCompatibilityAndOptionalRequestRecords() {
        BookInput legacyInput = new BookInput(
                "9780451524935",
                "1984",
                Genre.CLASSIC,
                "https://example.com/cover.jpg",
                List.of(1L)
        );
        assertNull(legacyInput.description());
        assertEquals(0, legacyInput.quantity());

        RefreshTokenInput refreshInput = new RefreshTokenInput("refresh-token");
        assertEquals("refresh-token", refreshInput.refreshToken());

        UserUpdateInput updateInput = new UserUpdateInput(
                "Updated name",
                "updated@example.com",
                "11999999999"
        );
        assertEquals("Updated name", updateInput.name());
        assertEquals("updated@example.com", updateInput.email());
        assertEquals("11999999999", updateInput.telephone());
    }

    @Test
    void shouldExposePersistedEntityMetadataAndIdentityEquality() {
        LocalDateTime registeredAt = LocalDateTime.of(2026, 11, 13, 10, 0);
        User user = new User();
        user.setDateRegister(registeredAt);
        assertEquals(registeredAt, user.getDateRegister());

        Book book = new Book();
        book.setCreatedAt(registeredAt);
        assertEquals(registeredAt, book.getCreatedAt());

        BookCopy copy = new BookCopy();
        copy.setCreatedAt(registeredAt);
        copy.setUpdatedAt(registeredAt.plusMinutes(1));
        assertEquals(registeredAt, copy.getCreatedAt());
        assertEquals(registeredAt.plusMinutes(1), copy.getUpdatedAt());

        Group group = new Group();
        group.setDescription("Library staff permissions");
        Permission permission = new Permission();
        permission.setId(1L);
        group.setPermissions(Set.of(permission));
        assertEquals("Library staff permissions", group.getDescription());
        assertEquals(Set.of(permission), group.getPermissions());

        Author author = new Author();
        author.setBooks(Set.of(book));
        assertEquals(Set.of(book), author.getBooks());

        Loan loan = new Loan();
        loan.setId(11L);
        loan.setRenewalCount(2);
        Loan sameLoan = new Loan();
        sameLoan.setId(11L);
        assertEquals(2, loan.getRenewalCount());
        assertEquals(loan, sameLoan);
        assertEquals(loan.hashCode(), sameLoan.hashCode());

        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshToken token = new RefreshToken();
        token.setId(21L);
        token.setUser(user);
        token.setJti("jti-21");
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(Instant.now());

        assertEquals(21L, token.getId());
        assertSame(user, token.getUser());
        assertEquals("jti-21", token.getJti());
        assertEquals("a".repeat(64), token.getTokenHash());
        assertEquals(expiresAt, token.getExpiresAt());
        assertNotNull(token.getCreatedAt());
        assertTrue(token.isActive());

        RefreshToken sameToken = new RefreshToken();
        sameToken.setId(21L);
        assertEquals(token, sameToken);
        assertEquals(token.hashCode(), sameToken.hashCode());
    }
}
