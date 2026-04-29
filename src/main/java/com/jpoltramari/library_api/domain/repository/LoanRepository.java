package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.model.Loan;

import java.util.List;

public interface LoanRepository extends CustomJpaRepository<Loan, Long>{

    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookId(Long bookId);
}
