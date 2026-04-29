package com.jpoltramari.library_api.api.assembler;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.dto.model.AuthorModel;
import com.jpoltramari.library_api.domain.model.Author;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthorAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public AuthorModel toModel(Author author){
        return modelMapper.map(author, AuthorModel.class);
    }

    public Author toEntity(AuthorInput input){
        return modelMapper.map(input, Author.class);
    }

    public void copyToEntity(AuthorInput input, Author author){
        modelMapper.map(input, author);
    }
}