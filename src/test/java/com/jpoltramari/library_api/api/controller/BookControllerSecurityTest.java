package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class BookControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService service;

    @MockBean
    private BookMapper mapper;

    @Test
    void shouldRejectBookListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBookListWithoutReadPermission() throws Exception {
        mockMvc.perform(get("/books")
                        .with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidBookId() throws Exception {
        mockMvc.perform(get("/books/0")
                        .with(user("user")
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("BOOK_READ"))))
                .andExpect(status().isBadRequest());
    }
}
