package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.application.exception.RateLimitKeyGenerationException;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Objects;
import java.util.function.LongSupplier;

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
    private final LongSupplier nanoTime;

    @Autowired
    public PublicRateLimitService(
            @Value("${app.security.rate-limit.login-ip.max-attempts:30}") int loginIpLimit,
            @Value("${app.security.rate-limit.login-ip.window:PT1M}") Duration loginIpWindow,
            @Value("${app.security.rate-limit.registration-ip.max-attempts:5}") int registrationIpLimit,
            @Value("${app.security.rate-limit.registration-ip.window:PT1H}") Duration registrationIpWindow,
            @Value("${app.security.rate-limit.account.max-failed-attempts:5}") int accountFailureLimit,
            @Value("${app.security.rate-limit.account.window:PT15M}") Duration accountFailureWindow,
            @Value("${jwt.secret}") String hmacSecret) {
        this(loginIpLimit, loginIpWindow, registrationIpLimit, registrationIpWindow,
                accountFailureLimit, accountFailureWindow, hmacSecret, System::nanoTime);
    }

    PublicRateLimitService(int loginIpLimit, Duration loginIpWindow,
                           int registrationIpLimit, Duration registrationIpWindow,
                           int accountFailureLimit, Duration accountFailureWindow,
                           String hmacSecret, LongSupplier nanoTime) {
        this.loginIpLimit = loginIpLimit;
        this.loginIpWindow = loginIpWindow;
        this.registrationIpLimit = registrationIpLimit;
        this.registrationIpWindow = registrationIpWindow;
        this.accountFailureLimit = accountFailureLimit;
        this.accountFailureWindow = accountFailureWindow;
        this.hmacSecret = hmacSecret;
        this.nanoTime = Objects.requireNonNull(nanoTime);
    }

    public boolean allowLoginFromIp(String ipAddress) {
        return acquire(loginIpWindows, "ip:" + safeIp(ipAddress), loginIpLimit, loginIpWindow);
    }

    public boolean allowRegistrationFromIp(String ipAddress) {
        return acquire(registrationIpWindows, "ip:" + safeIp(ipAddress),
                registrationIpLimit, registrationIpWindow);
    }

    public long loginIpRetryAfterSeconds(String ipAddress) {
        return retryAfterSeconds(loginIpWindows, "ip:" + safeIp(ipAddress),
                loginIpLimit, loginIpWindow);
    }

    public long registrationIpRetryAfterSeconds(String ipAddress) {
        return retryAfterSeconds(registrationIpWindows, "ip:" + safeIp(ipAddress),
                registrationIpLimit, registrationIpWindow);
    }

    public long accountRetryAfterSeconds(String email) {
        if (accountFailureLimit < 1 || accountFailureWindow.isZero()
                || accountFailureWindow.isNegative()) {
            return durationSecondsCeiling(accountFailureWindow);
        }
        Window window = accountFailureWindows.get(accountKey(email));
        return window == null ? 0 : window.retryAfterSeconds(accountFailureLimit, accountFailureWindow);
    }

    private long retryAfterSeconds(ConcurrentHashMap<String, Window> windows, String key,
                                  int limit, Duration duration) {
        if (limit < 1 || duration.isZero() || duration.isNegative()) {
            return 1;
        }
        Window window = windows.get(key);
        if (window == null) {
            return PublicRateLimitService.durationSecondsCeiling(duration);
        }
        long remaining = window.retryAfterSeconds(limit, duration);
        return remaining > 0 ? remaining : durationSecondsCeiling(duration);
    }

    private static long durationSecondsCeiling(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return 1;
        }
        long seconds = duration.getSeconds();
        return duration.getNano() == 0 ? Math.max(1, seconds) : Math.max(1, seconds + 1);
    }

    public boolean isAccountBlocked(String email) {
        String key = accountKey(email);
        if (accountFailureLimit < 1 || accountFailureWindow.isZero() || accountFailureWindow.isNegative()) {
            return true;
        }
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
        cleanupOccasionally(windows, duration);
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= MAX_TRACKED_KEYS) {
                cleanupExpired(windows, duration);
                if (windows.size() >= MAX_TRACKED_KEYS) {
                    return false;
                }
            }
            Window candidate = new Window(nanoTime);
            Window existing = windows.putIfAbsent(key, candidate);
            window = existing == null ? candidate : existing;
        }
        return window.tryAcquire(limit, duration);
    }

    private void cleanupOccasionally(ConcurrentHashMap<String, Window> windows, Duration duration) {
        if (operations.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpired(windows, duration);
        }
    }

    private void cleanupExpired(ConcurrentHashMap<String, Window> windows, Duration duration) {
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(duration));
    }

    private String accountKey(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return "account:" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new RateLimitKeyGenerationException(ex);
        }
    }

    private String safeIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress;
    }

    private static final class Window {
        private long startedAtNanos;
        private final LongSupplier nanoTime;

        private Window(LongSupplier nanoTime) {
            this.nanoTime = nanoTime;
            this.startedAtNanos = nanoTime.getAsLong();
        }
        private int count;
        private boolean evicted;

        synchronized boolean tryAcquire(int limit, Duration duration) {
            if (evicted) {
                return false;
            }
            resetIfExpired(duration);
            if (evicted || count >= limit) {
                return false;
            }
            count++;
            return true;
        }

        synchronized boolean isBlocked(int limit, Duration duration) {
            if (evicted) {
                return true;
            }
            resetIfExpired(duration);
            return evicted || count >= limit;
        }

        synchronized boolean isExpired(Duration duration) {
            if (evicted || nanoTime.getAsLong() - startedAtNanos >= duration.toNanos()) {
                evicted = true;
                return true;
            }
            return false;
        }

        synchronized long retryAfterSeconds(int limit, Duration duration) {
            if (evicted) {
                return durationSecondsCeiling(duration);
            }
            resetIfExpired(duration);
            if (evicted || count < limit) {
                return 0;
            }
            long elapsed = nanoTime.getAsLong() - startedAtNanos;
            long remainingNanos = Math.max(0, duration.toNanos() - elapsed);
            long wholeSeconds = remainingNanos / 1_000_000_000L;
            return Math.max(1, wholeSeconds + (remainingNanos % 1_000_000_000L == 0 ? 0 : 1));
        }

        private void resetIfExpired(Duration duration) {
            long now = nanoTime.getAsLong();
            if (!evicted && now - startedAtNanos >= duration.toNanos()) {
                startedAtNanos = now;
                count = 0;
            }
        }
    }
}
