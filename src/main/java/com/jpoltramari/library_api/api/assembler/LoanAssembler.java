package com.jpoltramari.library_api.api.assembler;

import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.domain.model.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanAssembler {

    public LoanModel toModel(Loan loan){
        LoanModel model = new LoanModel();

        model.setId(loan.getId());
        model.setStatus(loan.getStatus());

        model.setBookTitle(loan.getBook().getTitle());
        model.setUserName(loan.getUser().getName());

        model.setRequestDate(loan.getRequestDate());
        model.setApprovalDate(loan.getApprovalDate());
        model.setWithdrawDate(loan.getWithdrawableDate());
        model.setDueDate(loan.getDueDate());
        model.setReturnDate(loan.getReturnDate());

        return model;
    }
}