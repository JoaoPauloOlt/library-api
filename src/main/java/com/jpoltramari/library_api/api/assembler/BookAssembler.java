package com.jpoltramari.library_api.api.assembler;

import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.api.dto.model.BookModel;
import com.jpoltramari.library_api.domain.model.Book;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BookAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public BookModel toModel(Book book){
        return modelMapper.map(book, BookModel.class);
    }

    public Book toEntity(BookInput input){
        return modelMapper.map(input, Book.class);
    }

    public void copyToEntity(BookInput input, Book book){
        modelMapper.map(input, book);
    }
}