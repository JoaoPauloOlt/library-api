package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.service.LoanService;
import com.jpoltramari.library_api.infrastructure.security.AuthenticatedUser;
import com.jpoltramari.library_api.infrastructure.security.JwtAuthenticationFilter;
import com.jpoltramari.library_api.infrastructure.security.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class LoanControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService service;

    @MockBean
    private LoanMapper mapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldRequireAuthenticationForOwnLoans() throws Exception {
        mockMvc.perform(get("/loans/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserToListOwnLoans() throws Exception {
        AuthenticatedUser principal = principal(42L);
        when(service.findByUserId(eq(42L), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/loans/my").with(user(principal)))
                .andExpect(status().isOk());

        verify(service).findByUserId(eq(42L), any());
    }

    @Test
    void shouldRequireLoanReadAllPermissionForGlobalLoanList() throws Exception {
        mockMvc.perform(get("/loans").with(user(principal(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGlobalLoanListWithPermission() throws Exception {
        Loan loan = new Loan();
        loan.setId(10L);
        LoanModel model = new LoanModel(
                10L, "REQUESTED", "1984", null, List.of("George Orwell"),
                "Library User", null, null, null, null, null
        );

        when(service.findAll(any())).thenReturn(new PageImpl<>(List.of(loan)));
        when(mapper.toModel(loan)).thenReturn(model);

        mockMvc.perform(get("/loans")
                        .with(user(principal(42L, "LOAN_READ_ALL"))))
                .andExpect(status().isOk());
    }

    private AuthenticatedUser principal(Long userId, String... permissions) {
        JwtClaims claims = new JwtClaims(
                "jti-1",
                userId,
                "user@library.com",
                "Library User",
                List.of("USER"),
                List.of(permissions),
                0,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "library-api",
                "library-api"
        );
        return AuthenticatedUser.fromClaims(claims);
    }
}
