package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.model.Fine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    Page<Fine> findByUserId(Long userId, Pageable pageable);

    Page<Fine> findByUserIdAndStatus(Long userId, FineStatus status, Pageable pageable);

    Optional<Fine> findByLoanId(Long loanId);

    boolean existsByLoanId(Long loanId);
}
