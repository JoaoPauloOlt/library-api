package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.mapper.BookCopyMapper;
import com.jpoltramari.library_api.domain.service.BookCopyService;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookCopyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookCopyService service;

    @MockBean
    private BookCopyMapper mapper;

    @Test
    @WithMockUser(authorities = "BOOK_COPY_READ")
    void shouldAllowCopyDetailWithReadPermission() throws Exception {
        mockMvc.perform(get("/books/1/copies/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_READ")
    void shouldRejectInvalidCopyIdentifier() throws Exception {
        mockMvc.perform(get("/books/1/copies/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedCopyListing() throws Exception {
        mockMvc.perform(get("/books/1/copies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_CREATE")
    void shouldRejectCopyListingWithoutReadPermission() throws Exception {
        mockMvc.perform(get("/books/1/copies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_READ")
    void shouldAllowCopyListingWithReadPermission() throws Exception {
        when(service.findAllByBook(1L)).thenReturn(List.of());

        mockMvc.perform(get("/books/1/copies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_READ")
    void shouldRejectInvalidBookIdentifier() throws Exception {
        mockMvc.perform(get("/books/0/copies"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_READ")
    void shouldAllowAvailabilityWithReadPermission() throws Exception {
        when(service.totalQuantity(1L)).thenReturn(5L);
        when(service.availableQuantity(1L)).thenReturn(3L);

        mockMvc.perform(get("/books/1/copies/availability"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "BOOK_COPY_UPDATE")
    void shouldAllowStatusChangeOnlyWithUpdatePermission() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/books/1/copies/1/status")
                        .param("status", CopyStatus.AVAILABLE.name()))
                .andExpect(status().isOk());
    }
}
