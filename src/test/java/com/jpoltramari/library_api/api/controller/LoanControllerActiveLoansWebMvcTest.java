package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class LoanControllerActiveLoansWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LoanService loanService;
    @MockitoBean private LoanMapper loanMapper;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/loans/my/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnOnlyAuthenticatedUsersOpenLoans() throws Exception {
        when(loanService.findActiveByUserId(eq(42L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/loans/my/active")
                        .param("page", "0")
                        .param("size", "5")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isOk());

        verify(loanService).findActiveByUserId(eq(42L), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 5));
    }

    private static final class TestPrincipal implements UserDetails, AuthenticatedPrincipal {
        private final Long userId;

        private TestPrincipal(Long userId) {
            this.userId = userId;
        }

        @Override public Long getUserId() { return userId; }
        @Override public boolean hasPermission(String permission) { return false; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("USER"));
        }
        @Override public String getPassword() { return null; }
        @Override public String getUsername() { return "loan-user@library.com"; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    }
}
