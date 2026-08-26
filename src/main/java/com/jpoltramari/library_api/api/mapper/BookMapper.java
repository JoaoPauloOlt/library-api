package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.book.BookModel;
import com.jpoltramari.library_api.api.dto.book.BookUpdateInput;
import com.jpoltramari.library_api.domain.model.Book;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = AuthorMapper.class)
public interface BookMapper {

    BookModel toModel(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "copies", ignore = true)
    Book toEntity(BookInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "copies", ignore = true)
    void update(BookUpdateInput input, @MappingTarget Book book);
}
