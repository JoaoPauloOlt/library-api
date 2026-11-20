package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.service.FineService;
import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.model.Fine;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class FineControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FineService fineService;

    @Test
    void shouldRequireAuthenticationToListOwnFines() throws Exception {
        mockMvc.perform(get("/fines/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireFineReadOwnPermissionToListOwnFines() throws Exception {
        mockMvc.perform(get("/fines/my")
                        .with(authentication(authenticationFor(42L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListOnlyAuthenticatedUsersFines() throws Exception {
        when(fineService.findMyFines(eq(42L), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(fine())));

        mockMvc.perform(get("/fines/my")
                        .with(authentication(authenticationFor(42L, "FINE_READ_OWN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(15L))
                .andExpect(jsonPath("$.content[0].userId").value(42L))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        verify(fineService).findMyFines(eq(42L), eq(null), any());
    }

    @Test
    void shouldRequireAuthenticationToListAllFines() throws Exception {
        mockMvc.perform(get("/fines"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireFineReadAllPermissionToListAllFines() throws Exception {
        mockMvc.perform(get("/fines")
                        .with(authentication(authenticationFor(7L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListAllFinesWithDedicatedPermission() throws Exception {
        when(fineService.findAll(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(fine())));

        mockMvc.perform(get("/fines")
                        .with(authentication(authenticationFor(7L, "FINE_READ_ALL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(15L));

        verify(fineService).findAll(eq(null), any());
    }

    @Test
    void shouldRequireAuthenticationToReadFineDetails() throws Exception {
        mockMvc.perform(get("/fines/15"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnVisibleFineToItsOwner() throws Exception {
        when(fineService.findVisibleFine(eq(15L), any())).thenReturn(fine());

        mockMvc.perform(get("/fines/15")
                        .with(authentication(authenticationFor(42L, "FINE_READ_OWN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.loanId").value(9L))
                .andExpect(jsonPath("$.userId").value(42L));

        verify(fineService).findVisibleFine(eq(15L), argThat(principal ->
                principal.getUserId().equals(42L)
                        && principal.hasPermission("FINE_READ_OWN")));
    }

    @Test
    void shouldRejectInvalidFineId() throws Exception {
        mockMvc.perform(get("/fines/0")
                        .with(authentication(authenticationFor(42L, "FINE_READ_OWN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationToWaiveFine() throws Exception {
        mockMvc.perform(put("/fines/15/waive")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"Administrative review\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireFineWaivePermission() throws Exception {
        mockMvc.perform(put("/fines/15/waive")
                        .with(authentication(authenticationFor(7L)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"Administrative review\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectBlankWaiverReason() throws Exception {
        mockMvc.perform(put("/fines/15/waive")
                        .with(authentication(authenticationFor(7L, "FINE_WAIVE")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldWaiveFineWithDedicatedPermission() throws Exception {
        Fine waived = fine();
        waived.setStatus(FineStatus.WAIVED);
        waived.setWaivedAt(Instant.parse("2026-11-20T12:00:00Z"));
        waived.setWaiverReason("Administrative review");
        User administrator = new User();
        administrator.setId(7L);
        waived.setWaivedBy(administrator);
        when(fineService.waive(15L, "Administrative review", 7L)).thenReturn(waived);

        mockMvc.perform(put("/fines/15/waive")
                        .with(authentication(authenticationFor(7L, "FINE_WAIVE")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"Administrative review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAIVED"))
                .andExpect(jsonPath("$.waiverReason").value("Administrative review"))
                .andExpect(jsonPath("$.waivedBy").value(7L));

        verify(fineService).waive(15L, "Administrative review", 7L);
    }

    private Fine fine() {
        User borrower = new User();
        borrower.setId(42L);
        Loan loan = new Loan();
        loan.setId(9L);
        loan.setUser(borrower);

        Fine fine = new Fine();
        fine.setId(15L);
        fine.setLoan(loan);
        fine.setUser(borrower);
        fine.setLateDays(3);
        fine.setDailyRate(new BigDecimal("2.00"));
        fine.setAmount(new BigDecimal("6.00"));
        fine.setStatus(FineStatus.OPEN);
        fine.setCreatedAt(Instant.parse("2026-11-20T10:00:00Z"));
        return fine;
    }

    private Authentication authenticationFor(Long userId, String... permissions) {
        TestPrincipal principal = new TestPrincipal(userId, permissions);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
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

        @Override public String getPassword() { return null; }
        @Override public String getUsername() { return "fine-test@example.com"; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    }
}
