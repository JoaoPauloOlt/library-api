package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.LibraryApiApplication;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.readmodel.DashboardGenreMetric;
import com.jpoltramari.library_api.application.readmodel.DashboardSummary;
import com.jpoltramari.library_api.domain.enums.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.jpoltramari.library_api.application.service.DashboardService;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LibraryApiApplication.class)
@AutoConfigureMockMvc
class DashboardControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DashboardService dashboardService;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard/my-summary")
                        .param("from", "2026-11-01")
                        .param("to", "2026-12-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnSummaryForAuthenticatedUser() throws Exception {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 12, 1);
        when(dashboardService.getMySummary(42L, from, to))
                .thenReturn(new DashboardSummary(120L, 14L, 3L));

        mockMvc.perform(get("/dashboard/my-summary")
                        .param("from", "2026-11-01")
                        .param("to", "2026-12-01")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBooks").value(120))
                .andExpect(jsonPath("$.monthlyLoans").value(14))
                .andExpect(jsonPath("$.overdueLoans").value(3));

        verify(dashboardService).getMySummary(42L, from, to);
    }

    @Test
    void shouldRejectInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/dashboard/my-summary")
                        .param("from", "not-a-date")
                        .param("to", "2026-12-01")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsNotAfterStartDate() throws Exception {
        LocalDate from = LocalDate.of(2026, 12, 1);
        LocalDate to = LocalDate.of(2026, 11, 1);
        when(dashboardService.getMySummary(42L, from, to))
                .thenThrow(new BusinessException("The 'to' date must be after the 'from' date."));

        mockMvc.perform(get("/dashboard/my-summary")
                        .param("from", "2026-12-01")
                        .param("to", "2026-11-01")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE"));
    }

    @Test
    void shouldReturnGenreMetrics() throws Exception {
        when(dashboardService.getGenreMetrics())
                .thenReturn(List.of(new DashboardGenreMetric(Genre.FANTASY, 12L)));

        mockMvc.perform(get("/dashboard/genres")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre").value("FANTASY"))
                .andExpect(jsonPath("$[0].loanCount").value(12));
    }

    @Test
    void shouldReturnAvailableHighlightsPage() throws Exception {
        when(dashboardService.getAvailableHighlights(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/dashboard/available-highlights")
                        .param("page", "0")
                        .param("size", "5")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldReturnMostBorrowedBooksPage() throws Exception {
        when(dashboardService.getMostBorrowed(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/dashboard/most-borrowed")
                        .param("page", "0")
                        .param("size", "5")
                        .with(user(new TestPrincipal(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
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
        @Override public String getUsername() { return "dashboard-user@library.com"; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    }
}