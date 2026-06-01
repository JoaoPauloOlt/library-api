package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.model.BookCopy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

    boolean existsByBarcode(String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select bc
        from BookCopy bc
        where bc.book.id = :bookId
            and bc.active = true
            and bc.status = com.jpoltramari.library_api.domain.enums.CopyStatus.AVAILABLE
        order by bc.id
""")
    Optional<BookCopy> findFirstAvailableCopy(Long bookId);
}
