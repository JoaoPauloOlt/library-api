package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void shouldRequireAuthenticationForOwnLoans() throws Exception {
        mockMvc.perform(get("/loans/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedUserToListOwnLoans() throws Exception {
        AuthenticatedPrincipal principal = principal(42L);
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

    @Test
    void shouldRequireAuthenticationForLoanDetail() throws Exception {
        mockMvc.perform(get("/loans/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireLoanReadAllPermissionForLoanDetail() throws Exception {
        mockMvc.perform(get("/loans/1").with(user(principal(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowLoanDetailWithReadAllPermission() throws Exception {
        Loan loan = new Loan();
        loan.setId(10L);

        when(service.findOrFail(10L)).thenReturn(loan);

        mockMvc.perform(get("/loans/10")
                        .with(user(principal(42L, "LOAN_READ_ALL"))))
                .andExpect(status().isOk());

        verify(service).findOrFail(10L);
    }

    @Test
    void shouldRequireAuthenticationForLoanCreation() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireLoanCreatePermission() throws Exception {
        mockMvc.perform(post("/loans")
                        .with(user(principal(42L)))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateLoanWithCreatePermission() throws Exception {
        Loan loan = new Loan();
        loan.setId(10L);

        LoanModel model = new LoanModel(
                10L, "REQUESTED", "1984", null, List.of("George Orwell"),
                "Library User", null, null, null, null, null
        );

        when(service.create(any(), eq(42L))).thenReturn(loan);
        when(mapper.toModel(loan)).thenReturn(model);

        mockMvc.perform(post("/loans")
                        .with(user(principal(42L, "LOAN_CREATE")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"bookId\": 10}"))
                .andExpect(status().isCreated());

        verify(service).create(any(), eq(42L));
    }

    @Test
    void shouldRequireLoanApprovePermission() throws Exception {
        mockMvc.perform(put("/loans/1/approve")
                        .with(user(principal(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidLoanIdBeforeApproveAuthorization() throws Exception {
        mockMvc.perform(put("/loans/0/approve")
                        .with(user(principal(42L, "LOAN_APPROVE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireLoanReturnPermission() throws Exception {
        mockMvc.perform(put("/loans/1/return")
                        .with(user(principal(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidLoanIdBeforeReturnServiceCall() throws Exception {
        mockMvc.perform(put("/loans/0/return")
                        .with(user(principal(42L, "LOAN_RETURN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireLoanCancelPermission() throws Exception {
        mockMvc.perform(put("/loans/1/cancel")
                        .with(user(principal(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidLoanIdBeforeCancelServiceCall() throws Exception {
        mockMvc.perform(put("/loans/0/cancel")
                        .with(user(principal(42L, "LOAN_CANCEL"))))
                .andExpect(status().isBadRequest());
    }

    private AuthenticatedPrincipal principal(Long userId, String... permissions) {
        return new TestPrincipal(userId, permissions);
    }

    private static final class TestPrincipal implements UserDetails, AuthenticatedPrincipal {

        private final Long userId;
        private final List<GrantedAuthority> authorities;

        private TestPrincipal(Long userId, String... permissions) {
            this.userId = userId;
            this.authorities = List.of(permissions).stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(authority -> (GrantedAuthority) authority)
                    .toList();
        }

        @Override
        public Long getUserId() {
            return userId;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return "user@library.com";
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
