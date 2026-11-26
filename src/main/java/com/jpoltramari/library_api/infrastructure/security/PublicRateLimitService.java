package com.jpoltramari.library_api.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Duration;

@Service
public class PublicRateLimitService {

    private static final int MAX_TRACKED_KEYS = 50_000;
    private static final int CLEANUP_INTERVAL = 128;

    private final ConcurrentHashMap<String, Window> loginIpWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> registrationIpWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> accountFailureWindows = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();

    private final int loginIpLimit;
    private final Duration loginIpWindow;
    private final int registrationIpLimit;
    private final Duration registrationIpWindow;
    private final int accountFailureLimit;
    private final Duration accountFailureWindow;
    private final String hmacSecret;

    public PublicRateLimitService(
            @Value("${app.security.rate-limit.login-ip.max-attempts:30}") int loginIpLimit,
            @Value("${app.security.rate-limit.login-ip.window:PT1M}") Duration loginIpWindow,
            @Value("${app.security.rate-limit.registration-ip.max-attempts:5}") int registrationIpLimit,
            @Value("${app.security.rate-limit.registration-ip.window:PT1H}") Duration registrationIpWindow,
            @Value("${app.security.rate-limit.account.max-failed-attempts:5}") int accountFailureLimit,
            @Value("${app.security.rate-limit.account.window:PT15M}") Duration accountFailureWindow,
            @Value("${jwt.secret}") String hmacSecret) {
        this.loginIpLimit = loginIpLimit;
        this.loginIpWindow = loginIpWindow;
        this.registrationIpLimit = registrationIpLimit;
        this.registrationIpWindow = registrationIpWindow;
        this.accountFailureLimit = accountFailureLimit;
        this.accountFailureWindow = accountFailureWindow;
        this.hmacSecret = hmacSecret;
    }

    public boolean allowLoginFromIp(String ipAddress) {
        return acquire(loginIpWindows, "ip:" + safeIp(ipAddress), loginIpLimit, loginIpWindow);
    }

    public boolean allowRegistrationFromIp(String ipAddress) {
        return acquire(registrationIpWindows, "ip:" + safeIp(ipAddress),
                registrationIpLimit, registrationIpWindow);
    }

    public boolean isAccountBlocked(String email) {
        String key = accountKey(email);
        Window window = accountFailureWindows.get(key);
        return window != null && window.isBlocked(accountFailureLimit, accountFailureWindow);
    }

    public void recordFailedLogin(String email) {
        acquire(accountFailureWindows, accountKey(email), accountFailureLimit, accountFailureWindow);
    }

    public void resetFailedLogins(String email) {
        accountFailureWindows.remove(accountKey(email));
    }

    private boolean acquire(ConcurrentHashMap<String, Window> windows, String key,
                            int limit, Duration duration) {
        if (limit < 1 || duration.isZero() || duration.isNegative()) {
            return false;
        }
        cleanupOccasionally(windows);
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= MAX_TRACKED_KEYS) {
                cleanupExpired(windows);
                if (windows.size() >= MAX_TRACKED_KEYS) {
                    return false;
                }
            }
            Window candidate = new Window();
            Window existing = windows.putIfAbsent(key, candidate);
            window = existing == null ? candidate : existing;
        }
        return window.tryAcquire(limit, duration);
    }

    private void cleanupOccasionally(ConcurrentHashMap<String, Window> windows) {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpired(windows);
        }
    }

    private void cleanupExpired(ConcurrentHashMap<String, Window> windows) {
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String accountKey(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return "account:" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to create account rate-limit key", ex);
        }
    }

    private String safeIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
    }

    private static final class Window {
        private long startedAtNanos = System.nanoTime();
        private int count;

        synchronized boolean tryAcquire(int limit, Duration duration) {
            resetIfExpired(duration);
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }

        synchronized boolean isBlocked(int limit, Duration duration) {
            resetIfExpired(duration);
            return count >= limit;
        }

        synchronized boolean isExpired() {
            return System.nanoTime() - startedAtNanos >= Duration.ofHours(2).toNanos();
        }

        private void resetIfExpired(Duration duration) {
            if (System.nanoTime() - startedAtNanos >= duration.toNanos()) {
                startedAtNanos = System.nanoTime();
                count = 0;
            }
        }
    }
}
