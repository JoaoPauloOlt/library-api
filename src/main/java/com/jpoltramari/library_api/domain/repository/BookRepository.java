package com.jpoltramari.library_api.domain.repository;

import com.jpoltramari.library_api.application.readmodel.DashboardGenreMetric;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findAllByTitleContaining(String title);
    Optional<Book> findByTitle(String title);
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    @Query("""
        select new com.jpoltramari.library_api.application.readmodel.DashboardGenreMetric(
            b.genre, count(l.id)
        )
        from Book b
        left join Loan l on l.bookCopy.book = b and l.status in :loanStatuses
        group by b.genre
        order by count(l.id) desc, b.genre asc
    """)
    List<DashboardGenreMetric> findDashboardGenreMetrics(
            @Param("loanStatuses") Collection<LoanStatus> loanStatuses);

    @Query(
        value = """
            select b
            from Book b
            left join b.copies c
            left join Loan l on l.bookCopy = c and l.status in :loanStatuses
            where b.genre = :genre
            group by b
            order by count(l.id) desc, b.title asc
        """,
        countQuery = "select count(b.id) from Book b where b.genre = :genre"
    )
    Page<Book> findDashboardGenreBooks(
            @Param("genre") Genre genre,
            @Param("loanStatuses") Collection<LoanStatus> loanStatuses,
            Pageable pageable);

    @Query(
        value = """
            select b
            from Book b
            left join b.copies c
            left join Loan l on l.bookCopy = c and l.status in :loanStatuses
            group by b
            order by count(l.id) desc, b.title asc
        """,
        countQuery = "select count(b.id) from Book b"
    )
    Page<Book> findDashboardMostBorrowed(
            @Param("loanStatuses") Collection<LoanStatus> loanStatuses,
            Pageable pageable);

    @Query(
        value = """
            select b
            from Book b
            join b.copies c
            left join Loan l on l.bookCopy = c and l.status in :loanStatuses
            where c.active = true and c.status = :availableStatus
            group by b
            order by count(l.id) desc, b.title asc
        """,
        countQuery = """
            select count(distinct b.id)
            from Book b
            join b.copies c
            where c.active = true and c.status = :availableStatus
        """
    )
    Page<Book> findDashboardAvailableHighlights(
            @Param("availableStatus") CopyStatus availableStatus,
            @Param("loanStatuses") Collection<LoanStatus> loanStatuses,
            Pageable pageable);
}
