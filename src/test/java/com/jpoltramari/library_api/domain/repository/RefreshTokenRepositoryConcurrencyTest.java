package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.RefreshToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RefreshTokenRepositoryConcurrencyTest {

    private static final Long USER_ID = 999_001L;
    private static final String TOKEN_HASH = "a".repeat(64);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);

        jdbcTemplate.update(
                "insert into users (id, name, email, password, telephone, status, token_version) " +
                "values (?, ?, ?, ?, ?, ?, ?)",
                USER_ID, "Lock Test User", "lock-test@example.com", "password",
                "11999999999", "ACTIVE", 0
        );

        jdbcTemplate.update(
                "insert into refresh_tokens (id, user_id, jti, token_hash, expires_at) " +
                "values (?, ?, ?, ?, ?)",
                USER_ID, USER_ID, "lock-test-jti", TOKEN_HASH,
                java.sql.Timestamp.from(Instant.now().plusSeconds(600))
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from refresh_tokens where user_id = ?", USER_ID);
        jdbcTemplate.update("delete from users where id = ?", USER_ID);
        executor.shutdownNow();
    }

    @Test
    void shouldSerializeConcurrentRefreshTokenReadsWithPessimisticLock() throws Exception {
        CountDownLatch firstTransactionLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        Future<RefreshToken> first = executor.submit(() ->
                transactionTemplate.execute(status -> {
                    RefreshToken token = refreshTokenRepository.findByTokenHash(TOKEN_HASH)
                            .orElseThrow();

                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.saveAndFlush(token);
                    firstTransactionLocked.countDown();

                    await(releaseFirstTransaction);
                    return token;
                })
        );

        assertThat(firstTransactionLocked.await(5, TimeUnit.SECONDS)).isTrue();

        Future<RefreshToken> second = executor.submit(() ->
                transactionTemplate.execute(status ->
                        refreshTokenRepository.findByTokenHash(TOKEN_HASH).orElseThrow()
                )
        );

        Thread.sleep(300);
        assertThat(second.isDone())
                .as("the second transaction must remain blocked while the first transaction holds the row lock")
                .isFalse();

        releaseFirstTransaction.countDown();

        RefreshToken firstResult = first.get(5, TimeUnit.SECONDS);
        RefreshToken secondResult = second.get(5, TimeUnit.SECONDS);

        assertThat(firstResult.getRevokedAt()).isNotNull();
        assertThat(secondResult.getRevokedAt()).isNotNull();
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent transaction", e);
        }
    }
}
