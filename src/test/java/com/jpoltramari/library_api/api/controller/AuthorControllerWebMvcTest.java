package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.mapper.AuthorMapper;
import com.jpoltramari.library_api.application.service.AuthorService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class AuthorControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorService service;

    @MockBean
    private AuthorMapper mapper;

    @Test
    void shouldRejectAuthorListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/authors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthorListWithoutReadPermission() throws Exception {
        mockMvc.perform(get("/authors")
                        .with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuthorListWithReadPermission() throws Exception {
        when(service.findAll(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/authors")
                        .with(user("librarian")
                                .authorities(new SimpleGrantedAuthority("AUTHOR_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidAuthorId() throws Exception {
        mockMvc.perform(get("/authors/0")
                        .with(user("librarian")
                                .authorities(new SimpleGrantedAuthority("AUTHOR_READ"))))
                .andExpect(status().isBadRequest());
    }
}
