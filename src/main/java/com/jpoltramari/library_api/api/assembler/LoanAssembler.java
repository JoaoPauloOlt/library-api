package com.jpoltramari.library_api.api.assembler;

import com.jpoltramari.library_api.api.dto.model.LoanModel;
import com.jpoltramari.library_api.domain.model.Loan;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoanAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public LoanModel toModel(Loan loan){
        return modelMapper.map(loan, LoanModel.class);
    }
}