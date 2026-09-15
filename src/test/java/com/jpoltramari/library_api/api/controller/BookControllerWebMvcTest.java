package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.book.BookModel;
import com.jpoltramari.library_api.api.mapper.BookMapper;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.service.BookService;
import com.jpoltramari.library_api.infrastructure.security.ApiSecurityExceptionHandler;
import com.jpoltramari.library_api.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({
        com.jpoltramari.library_api.infrastructure.security.SecurityConfig.class,
        ApiSecurityExceptionHandler.class
})
class BookControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService service;

    @MockBean
    private BookMapper mapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void shouldRejectBookListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBookListWithoutBookReadPermission() throws Exception {
        mockMvc.perform(get("/books").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListBooksForUserWithBookReadPermission() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("1984");

        BookModel model = new BookModel(
                1L, "9780451524935", "1984", "SCIENCE_FICTION", null,
                null, "Dystopian novel", 2L, 1L, 3L, List.of()
        );

        when(service.findAll(any(), any())).thenReturn(new PageImpl<>(List.of(book)));
        when(mapper.toModel(book)).thenReturn(model);

        mockMvc.perform(get("/books")
                        .with(user("reader").authorities(new SimpleGrantedAuthority("BOOK_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectBookCreationWithInvalidInput() throws Exception {
        String invalidPayload = """
                {
                  "isbn": "123",
                  "title": "Invalid book",
                  "genre": "CLASSIC",
                  "quantity": 1,
                  "authorIds": [1]
                }
                """;

        mockMvc.perform(post("/books")
                        .with(user("librarian").authorities(new SimpleGrantedAuthority("BOOK_CREATE")))
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
