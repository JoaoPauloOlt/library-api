package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Loan;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends CustomJpaRepository<Loan, Long>{

    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookId(Long bookId);

    List<Loan> findByStatus(String status);

    @Query("""
        SELECT COUNT(1)
        FROM Loan l
        WHERE l.user.id = :userId
        AND l.status IN ('SOLICITADO', 'APROVADO', 'RETIRADO', 'ATRASADO')
""")
    long countActiveLoansByUser(Long userId);
}