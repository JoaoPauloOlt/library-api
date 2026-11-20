package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.OutboxStatus;
import com.jpoltramari.library_api.domain.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event
            from OutboxEvent event
            where event.status = :status
              and event.availableAt <= :now
            order by event.createdAt asc
            """)
    List<OutboxEvent> findAvailableForUpdate(
            @Param("status") OutboxStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update OutboxEvent event
            set event.status = :publishedStatus,
                event.publishedAt = :publishedAt,
                event.lastError = null
            where event.id = :id
              and event.status = :pendingStatus
              and event.availableAt = :leaseUntil
            """)
    int markPublished(
            @Param("id") UUID id,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("publishedAt") Instant publishedAt,
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("publishedStatus") OutboxStatus publishedStatus
    );

    @Query("select event.attempts from OutboxEvent event where event.id = :id")
    Optional<Integer> findAttemptsById(@Param("id") UUID id);

    @Modifying
    @Query("""
            update OutboxEvent event
            set event.attempts = event.attempts + 1,
                event.lastError = :lastError,
                event.availableAt = :retryAt,
                event.status = case
                    when event.attempts + 1 >= :maxAttempts then :failedStatus
                    else :pendingStatus
                end
            where event.id = :id
              and event.status = :pendingStatus
              and event.availableAt = :leaseUntil
            """)
    int markFailed(
            @Param("id") UUID id,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("pendingStatus") OutboxStatus pendingStatus,
            @Param("failedStatus") OutboxStatus failedStatus,
            @Param("retryAt") Instant retryAt,
            @Param("lastError") String lastError,
            @Param("maxAttempts") int maxAttempts
    );
}
