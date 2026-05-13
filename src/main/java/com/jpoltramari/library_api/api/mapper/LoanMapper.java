package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.domain.model.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    @Mapping(
            target = "status",
            expression = "java(loan.getStatus() != null ? loan.getStatus().name() : null)"
    )
    @Mapping(
            target = "bookTitle",
            expression = """
                java(
                    loan.getBookCopy() != null &&
                    loan.getBookCopy().getBook() != null
                        ? loan.getBookCopy().getBook().getTitle()
                        : null
                )
            """
    )
    @Mapping(
            target = "userName",
            expression = """
                java(
                    loan.getUser() != null
                        ? loan.getUser().getName()
                        : null
                )
            """
    )
    @Mapping(
            target = "withdrawDate",
            source = "withdrawableDate"
    )
    LoanModel toModel(Loan loan);
}