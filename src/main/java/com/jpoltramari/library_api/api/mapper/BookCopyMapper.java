package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyModel;
import com.jpoltramari.library_api.domain.model.BookCopy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookCopyMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "bookTitle", source = "book.title")
    BookCopyModel toModel(BookCopy copy);
}
