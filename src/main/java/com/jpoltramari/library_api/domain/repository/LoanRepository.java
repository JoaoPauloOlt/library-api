package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.model.Loan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LoanRepository extends CustomJpaRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookCopyBookId(Long bookId);

    List<Loan> findByStatus(LoanStatus status);

    @Query("""
        select count(l)
        from Loan l
        where l.user.id = :userId
          and l.status in :statuses
    """)
    long countActiveLoansByUser(Long userId,
                                Collection<LoanStatus> statuses);
}