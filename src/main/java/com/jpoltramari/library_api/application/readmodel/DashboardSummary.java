package com.jpoltramari.library_api.application.readmodel;

public record DashboardSummary(
        long totalBooks,
        long monthlyLoans,
        long overdueLoans
) {}
