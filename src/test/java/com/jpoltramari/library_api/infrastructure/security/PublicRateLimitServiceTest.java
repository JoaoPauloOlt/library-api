package com.jpoltramari.library_api.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
    void shouldResetWindowAfterConfiguredDuration() throws InterruptedException {
        PublicRateLimitService shortWindow = new PublicRateLimitService(
                1, Duration.ofMillis(20),
                1, Duration.ofHours(1),
                1, Duration.ofMinutes(15),
                "test-hmac-secret");

        assertTrue(shortWindow.allowLoginFromIp("192.0.2.50"));
        assertFalse(shortWindow.allowLoginFromIp("192.0.2.50"));
        Thread.sleep(30);
        assertTrue(shortWindow.allowLoginFromIp("192.0.2.50"));
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
