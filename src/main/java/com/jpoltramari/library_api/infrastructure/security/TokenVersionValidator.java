package com.jpoltramari.library_api.infrastructure.security;

import com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshot;
import com.jpoltramari.library_api.infrastructure.security.snapshot.UserSecuritySnapshotPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenVersionValidator {

    private final UserSecuritySnapshotPort snapshotPort;

    public ValidationResult validate(JwtClaims claims) {
        if (claims.userId() == null) {
            log.warn("event=jwt_rejected reason=missing_user_id correlationId={}", correlationId());
            return ValidationResult.INVALID;
        }

        var snapshot = snapshotPort.findByUserId(claims.userId());
        if (snapshot.isEmpty()) {
            log.warn("event=jwt_rejected reason=user_not_found userId={} correlationId={}",
                    claims.userId(), correlationId());
            return ValidationResult.INVALID;
        }

        UserSecuritySnapshot current = snapshot.get();

        if (!current.isActive()) {
            log.warn("event=jwt_rejected reason=user_inactive userId={} correlationId={}",
                    claims.userId(), correlationId());
            return ValidationResult.INACTIVE;
        }

        if (!Objects.equals(current.tokenVersion(), claims.tokenVersion())) {
            log.warn("event=jwt_rejected reason=token_version_mismatch userId={} claim={} current={} correlationId={}",
                    claims.userId(), claims.tokenVersion(), current.tokenVersion(), correlationId());
            return ValidationResult.VERSION_MISMATCH;
        }

        return ValidationResult.VALID;
    }

    private static String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    public enum ValidationResult {
        VALID,
        INVALID,
        INACTIVE,
        VERSION_MISMATCH
    }
}
