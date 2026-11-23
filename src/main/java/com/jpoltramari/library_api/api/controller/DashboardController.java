package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.dashboard.DashboardSummaryModel;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.readmodel.DashboardSummary;
import com.jpoltramari.library_api.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/my-summary")
    public DashboardSummaryModel getMySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        AuthenticatedPrincipal principal =
                (AuthenticatedPrincipal) authentication.getPrincipal();

        DashboardSummary summary = dashboardService.getMySummary(
                principal.getUserId(), from, to);

        return new DashboardSummaryModel(
                summary.totalBooks(),
                summary.monthlyLoans(),
                summary.overdueLoans()
        );
    }
}
