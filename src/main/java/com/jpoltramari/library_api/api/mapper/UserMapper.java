package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.dto.model.UserModel;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "groups", source = "groups", qualifiedByName = "mapGroups")
    @Mapping(target = "status", source = "status")
    UserModel toModel(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateRegister", ignore = true)
    @Mapping(target = "groups", ignore = true)
    User toEntity(UserInput input);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateRegister", ignore = true)
    @Mapping(target = "groups", ignore = true)
    void update(UserInput input, @MappingTarget User user);

    @Named("mapGroups")
    default List<String> mapGroups(Set<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        return groups.stream()
                .map(Group::getName)
                .toList();
    }

    default String mapStatus(Enum<?> status) {
        return status != null ? status.name() : null;
    }
}