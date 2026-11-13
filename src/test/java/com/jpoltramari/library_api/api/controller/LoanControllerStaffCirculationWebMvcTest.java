package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.mapper.LoanMapper;
import com.jpoltramari.library_api.application.command.loan.CreateLoanCommand;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.service.LoanService;
import com.jpoltramari.library_api.application.service.UserService;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class LoanControllerStaffCirculationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private UserService userService;

    @MockBean
    private LoanMapper loanMapper;

    @Test
    void shouldRequireAuthenticationToListActiveBorrowers() throws Exception {
        mockMvc.perform(get("/loans/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireStaffCirculationPermissionToListActiveBorrowers() throws Exception {
        mockMvc.perform(get("/loans/users").with(user(principal(7L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOnlyTheActiveBorrowerPageToAuthorizedStaff() throws Exception {
        User borrower = new User();
        borrower.setId(42L);
        borrower.setName("Library Borrower");
        borrower.setEmail("borrower@example.com");
        when(userService.findActiveBorrowers(any())).thenReturn(new PageImpl<>(List.of(borrower)));

        mockMvc.perform(get("/loans/users")
                        .with(user(principal(7L, "LOAN_CREATE_FOR_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42L))
                .andExpect(jsonPath("$.content[0].name").value("Library Borrower"))
                .andExpect(jsonPath("$.content[0].email").value("borrower@example.com"));

        verify(userService).findActiveBorrowers(any());
    }

    @Test
    void shouldRequireStaffCirculationPermissionToCreateLoanForAnotherUser() throws Exception {
        mockMvc.perform(post("/loans/for-user")
                        .with(user(principal(7L, "LOAN_CREATE")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"bookId\":10,\"userId\":42}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateLoanForSelectedBorrowerWithDedicatedPermission() throws Exception {
        Loan loan = new Loan();
        loan.setId(10L);
        LoanModel model = new LoanModel(
                10L, "REQUESTED", "1984", null, List.of("George Orwell"),
                "Library Borrower", null, null, null, null, null
        );
        when(loanService.createForUser(any(CreateLoanCommand.class), eq(42L))).thenReturn(loan);
        when(loanMapper.toModel(loan)).thenReturn(model);

        mockMvc.perform(post("/loans/for-user")
                        .with(authentication(authenticate(principal(7L, "LOAN_CREATE_FOR_USER"))))
                        .contentType(APPLICATION_JSON)
                        .content("{\"bookId\":10,\"userId\":42}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));

        verify(loanService).createForUser(any(CreateLoanCommand.class), eq(42L));
    }

    @Test
    void shouldRejectInvalidBookOrBorrowerIdentifiers() throws Exception {
        mockMvc.perform(post("/loans/for-user")
                        .with(user(principal(7L, "LOAN_CREATE_FOR_USER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"bookId\":0,\"userId\":42}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/loans/for-user")
                        .with(user(principal(7L, "LOAN_CREATE_FOR_USER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"bookId\":10,\"userId\":null}"))
                .andExpect(status().isBadRequest());
    }

    private Authentication authenticate(TestPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    private TestPrincipal principal(Long userId, String... permissions) {
        return new TestPrincipal(userId, permissions);
    }

    private static final class TestPrincipal implements UserDetails, AuthenticatedPrincipal {

        private final Long userId;
        private final List<GrantedAuthority> authorities;

        private TestPrincipal(Long userId, String... permissions) {
            this.userId = userId;
            this.authorities = List.of(permissions).stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        }

        @Override
        public Long getUserId() {
            return userId;
        }

        @Override
        public boolean hasPermission(String permission) {
            return authorities.stream()
                    .anyMatch(authority -> permission.equals(authority.getAuthority()));
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
            return "staff@library.com";
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
