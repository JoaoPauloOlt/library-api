package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.application.exception.RateLimitKeyGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PublicRateLimitServiceTest {

    private PublicRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new PublicRateLimitService(
                2, Duration.ofMinutes(1),
                2, Duration.ofHours(1),
                3, Duration.ofMinutes(15),
                "test-hmac-secret");
    }

    @Test
    void shouldLimitLoginAttemptsPerIpWithinWindow() {
        assertTrue(service.allowLoginFromIp("192.0.2.10"));
        assertTrue(service.allowLoginFromIp("192.0.2.10"));
        assertFalse(service.allowLoginFromIp("192.0.2.10"));
        assertTrue(service.allowLoginFromIp("192.0.2.11"));
    }

    @Test
    void shouldLimitRegistrationSeparatelyFromLogin() {
        assertTrue(service.allowRegistrationFromIp("192.0.2.10"));
        assertTrue(service.allowRegistrationFromIp("192.0.2.10"));
        assertFalse(service.allowRegistrationFromIp("192.0.2.10"));
        assertTrue(service.allowLoginFromIp("192.0.2.10"));
    }

    @Test
    void shouldBlockAccountAfterConfiguredFailedAttempts() {
        String email = "reader@example.com";

        assertFalse(service.isAccountBlocked(email));
        service.recordFailedLogin(email);
        service.recordFailedLogin(email);
        assertFalse(service.isAccountBlocked(email));
        service.recordFailedLogin(email);
        assertTrue(service.isAccountBlocked(email));

        service.resetFailedLogins(email);
        assertFalse(service.isAccountBlocked(email));
    }

    @Test
    void shouldUseNormalizedEmailForAccountLimitKey() {
        service.recordFailedLogin(" Reader@Example.com ");
        service.recordFailedLogin("reader@example.com");
        service.recordFailedLogin("READER@example.com");

        assertTrue(service.isAccountBlocked("reader@example.com"));
    }

    @Test
    void shouldTreatNullAndBlankIpAsSameClientKey() {
        assertTrue(service.allowLoginFromIp(null));
        assertTrue(service.allowLoginFromIp(""));
        assertFalse(service.allowLoginFromIp("   "));
    }

    @Test
    void shouldResetWindowAfterConfiguredDuration() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        PublicRateLimitService shortWindow = new PublicRateLimitService(
                1, Duration.ofMillis(20),
                1, Duration.ofHours(1),
                1, Duration.ofMinutes(15),
                "test-hmac-secret", clock::get);

        assertTrue(shortWindow.allowLoginFromIp("192.0.2.50"));
        assertFalse(shortWindow.allowLoginFromIp("192.0.2.50"));
        clock.addAndGet(Duration.ofMillis(21).toNanos());
        assertTrue(shortWindow.allowLoginFromIp("192.0.2.50"));
    }

    @Test
    void shouldRemoveExpiredBucketsDuringPeriodicCleanup() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        PublicRateLimitService cleanupService = new PublicRateLimitService(
                10, Duration.ofMillis(10),
                10, Duration.ofHours(1),
                10, Duration.ofMinutes(15),
                "test-hmac-secret", clock::get);

        assertTrue(cleanupService.allowLoginFromIp("expired-client"));
        clock.addAndGet(Duration.ofMillis(11).toNanos());
        for (int i = 0; i < 127; i++) {
            assertTrue(cleanupService.allowLoginFromIp("client-" + i));
        }
        assertTrue(cleanupService.allowLoginFromIp("expired-client"));
    }

    @Test
    void shouldReturnConfiguredRetryAfterRoundedUpToRemainingWindow() {
        AtomicLong clock = new AtomicLong(0);
        PublicRateLimitService configured = new PublicRateLimitService(
                1, Duration.ofMillis(2_500),
                1, Duration.ofMillis(5_500),
                1, Duration.ofMillis(7_500),
                "test-hmac-secret", clock::get);

        assertTrue(configured.allowLoginFromIp("192.0.2.90"));
        assertFalse(configured.allowLoginFromIp("192.0.2.90"));
        assertEquals(3, configured.loginIpRetryAfterSeconds("192.0.2.90"));

        clock.addAndGet(Duration.ofMillis(1_200).toNanos());
        assertEquals(2, configured.loginIpRetryAfterSeconds("192.0.2.90"));

        configured.recordFailedLogin("blocked@example.com");
        assertTrue(configured.isAccountBlocked("blocked@example.com"));
        assertEquals(8, configured.accountRetryAfterSeconds("blocked@example.com"));
    }

    @Test
    void shouldExposeSpecificExceptionForRateLimitKeyFailure() {
        IllegalArgumentException cause = new IllegalArgumentException("test");
        RateLimitKeyGenerationException exception = new RateLimitKeyGenerationException(cause);

        assertEquals("Unable to create account rate-limit key", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldFailClosedWhenLimitConfigurationIsInvalid() {
        PublicRateLimitService invalid = new PublicRateLimitService(
                0, Duration.ofMinutes(1),
                0, Duration.ofHours(1),
                0, Duration.ofMinutes(15),
                "test-hmac-secret");

        assertFalse(invalid.allowLoginFromIp("192.0.2.10"));
        assertFalse(invalid.allowRegistrationFromIp("192.0.2.10"));
        assertTrue(invalid.isAccountBlocked("reader@example.com"));
    }
}
