package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository
        extends CustomJpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    Long countByBookId(Long bookId);

    Long countByBookIdAndStatus(
            Long bookId,
            CopyStatus status
    );

    List<BookCopy> findAllByBookId(Long bookId);

    Optional<BookCopy> findByIdAndBookId(Long id, Long bookId);

    boolean existsByBarcode(String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BookCopy> findFirstByBookIdAndActiveTrueAndStatusOrderByIdAsc(
            Long bookId,
            CopyStatus status
    );
}
