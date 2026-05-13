package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.BookCopy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookCopyRepository
        extends CustomJpaRepository<BookCopy, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select bc
        from BookCopy bc
        where bc.book.id = :bookId
          and bc.status = com.jpoltramari.library_api.domain.enums.CopyStatus.AVAILABLE
        order by bc.id asc
    """)
    Optional<BookCopy> findFirstAvailableCopy(Long bookId);
}
