package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.input.AuthorInput;
import com.jpoltramari.library_api.api.dto.model.AuthorModel;
import com.jpoltramari.library_api.domain.model.Author;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    AuthorModel toModel(Author author);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    Author toEntity(AuthorInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "books", ignore = true)
    void update(AuthorInput input, @MappingTarget Author author);
}