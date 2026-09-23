package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.dto.user.UserModel;
import com.jpoltramari.library_api.api.mapper.UserMapper;
import com.jpoltramari.library_api.domain.model.User;
import com.jpoltramari.library_api.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService service;

    @MockBean
    private UserMapper mapper;

    @Test
    void shouldAllowPublicRegistration() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("João Paulo");
        user.setEmail("joao@example.com");

        when(service.create(any())).thenReturn(user);
        when(mapper.toModel(user)).thenReturn(
                new UserModel(1L, "João Paulo", "joao@example.com", "11999999999", "ACTIVE", List.of("USER"))
        );

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "João Paulo",
                                  "email": "joao@example.com",
                                  "telephone": "11999999999",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid",
                                  "telephone": "",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUserListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserListWithoutAdministrativePermission() throws Exception {
        mockMvc.perform(get("/users")
                        .with(user("librarian")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListUsersWithAdministrativePermission() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("João Paulo");

        when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(user)));
        when(mapper.toModel(user)).thenReturn(
                new UserModel(1L, "João Paulo", "joao@example.com", "11999999999", "ACTIVE", List.of("USER"))
        );

        mockMvc.perform(get("/users")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USER_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUserLookupWithoutAdministrativePermission() throws Exception {
        mockMvc.perform(get("/users/1")
                        .with(user("user")))
                .andExpect(status().isForbidden());
    }
}
