package com.jpoltramari.library_api.api.assembler;

import com.jpoltramari.library_api.api.dto.input.UserInput;
import com.jpoltramari.library_api.api.dto.model.UserModel;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {

    @Autowired
    private ModelMapper modelMapper;

    public UserModel toModel(User user){
        UserModel model = modelMapper.map(user, UserModel.class);

        model.setGroups(
                user.getGroups()
                        .stream()
                        .map(Group::getName)
                        .toList()
        );
        return model;
    }

    public User toEntity(UserInput input){
        return modelMapper.map(input, User.class);
    }

    public void copyToEntity(UserInput input, User user){
        modelMapper.map(input, user);
    }
}
