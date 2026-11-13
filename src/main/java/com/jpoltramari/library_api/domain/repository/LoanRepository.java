package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.Loan;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select loan from Loan loan where loan.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Loan loan
        set loan.status = :lateStatus
        where loan.status = :activeStatus
          and loan.dueDate <= :now
    """)
    int markOverdueLoans(
            @Param("now") LocalDateTime now,
            @Param("activeStatus") LoanStatus activeStatus,
            @Param("lateStatus") LoanStatus lateStatus
    );

    @EntityGraph(attributePaths = {"bookCopy", "bookCopy.book", "bookCopy.book.authors"})
    Page<Loan> findByUserId(Long userId, Pageable pageable);
    List<Loan> findByUserId(Long userId);
    List<Loan> findByBookCopyBookId(Long bookId);
    List<Loan> findByStatus(LoanStatus status);

    @Query("""
        select count(l)
        from Loan l
        where l.user.id = :userId
          and l.status in :statuses
    """)
    long countActiveLoansByUser(Long userId, Collection<LoanStatus> statuses);

    boolean existsByUserIdAndBookCopyBookIdAndStatusIn(Long userId, Long bookId, List<LoanStatus> statuses);
}
