package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import com.jpoltramari.library_api.domain.repository.LoanRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoanCreationConcurrencyIntegrationTest {

    @Autowired private LoanService loanService;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<Long> userIds = new ArrayList<>();
    private Long bookId;
    private Long copyId;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        for (int i = 0; i < 2; i++) {
            User user = new User();
            user.setName("Concurrent Loan User " + i);
            user.setEmail("concurrent-" + UUID.randomUUID() + "@example.com");
            user.setPassword("test-password");
            user.setTelephone("11999999999");
            user.setStatus(UserStatus.ACTIVE);
            user.setTokenVersion(0);
            userIds.add(userRepository.save(user).getId());
        }

        Book book = new Book();
        book.setIsbn(UUID.randomUUID().toString().replace("-", "").substring(0, 13));
        book.setTitle("Concurrent Reservation Book");
        book.setGenre(Genre.CLASSIC);
        bookId = bookRepository.save(book).getId();

        BookCopy copy = new BookCopy();
        copy.setBook(bookRepository.findById(bookId).orElseThrow());
        copy.setBarcode("IT-CONCURRENT-" + UUID.randomUUID());
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        copyId = bookCopyRepository.save(copy).getId();
    }

    @AfterEach
    void cleanUp() {
        if (bookId != null) {
            loanRepository.deleteAll(loanRepository.findByBookCopyBookId(bookId));
            if (copyId != null) {
                bookCopyRepository.deleteById(copyId);
            }
            bookRepository.deleteById(bookId);
        }
        if (!userIds.isEmpty()) {
            userRepository.deleteAllById(userIds);
            userIds.clear();
        }
    }

    @Test
    void shouldPreventTwoConcurrentRequestsFromReservingTheSameCopy() throws Exception {
        List<OperationResult> results = executeWhileCopyIsLocked(
                () -> loanService.create(new CreateLoanCommand(bookId), userIds.get(0)),
                () -> loanService.create(new CreateLoanCommand(bookId), userIds.get(1))
        );

        assertSingleSuccessAndBusinessFailure(results);
        assertThat(loanRepository.findByBookCopyBookId(bookId)).hasSize(1);
        assertThat(bookCopyRepository.findById(copyId).orElseThrow().getStatus())
                .isEqualTo(CopyStatus.RESERVED);
    }

    private List<OperationResult> executeWhileCopyIsLocked(
            Supplier<Loan> firstOperation,
            Supplier<Loan> secondOperation
    ) throws Exception {
        ExecutorService lockExecutor = Executors.newSingleThreadExecutor();
        ExecutorService operationExecutor = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch operationsReady = new CountDownLatch(2);

        try {
            Future<?> lockHolder = lockExecutor.submit(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        bookCopyRepository.findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(
                                bookId, CopyStatus.AVAILABLE
                        ).orElseThrow();
                        lockAcquired.countDown();
                        awaitRelease(releaseLock);
                    })
            );

            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();
            Future<OperationResult> first = operationExecutor.submit(
                    () -> executeOperation(firstOperation, operationsReady));
            Future<OperationResult> second = operationExecutor.submit(
                    () -> executeOperation(secondOperation, operationsReady));

            assertThat(operationsReady.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(200);
            releaseLock.countDown();

            List<OperationResult> results = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );
            lockHolder.get(15, TimeUnit.SECONDS);
            return results;
        } finally {
            releaseLock.countDown();
            lockExecutor.shutdownNow();
            operationExecutor.shutdownNow();
        }
    }

    private OperationResult executeOperation(Supplier<Loan> operation, CountDownLatch ready) {
        ready.countDown();
        try {
            return new OperationResult(operation.get(), null);
        } catch (RuntimeException exception) {
            return new OperationResult(null, exception);
        }
    }

    private void awaitRelease(CountDownLatch releaseLock) {
        try {
            if (!releaseLock.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the test row lock.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding the test row lock.", exception);
        }
    }

    private void assertSingleSuccessAndBusinessFailure(List<OperationResult> results) {
        assertThat(results.stream().filter(result -> result.loan() != null)).hasSize(1);
        List<OperationResult> failures = results.stream().filter(result -> result.failure() != null).toList();
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).failure()).isInstanceOf(BusinessException.class);
    }

    private record OperationResult(Loan loan, RuntimeException failure) {
    }
}
