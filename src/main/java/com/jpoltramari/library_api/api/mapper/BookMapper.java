package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.input.BookInput;
import com.jpoltramari.library_api.api.dto.model.BookModel;
import com.jpoltramari.library_api.domain.model.Book;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = AuthorMapper.class)
public interface BookMapper {

    @Mapping(target = "totalQuantity", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    BookModel toModel(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authors", ignore = true)
    Book toEntity(BookInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authors", ignore = true)
    void update(BookInput input, @MappingTarget Book book);
}