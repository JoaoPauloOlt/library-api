package com.jpoltramari.library_api.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DashboardSummaryModel", description = "Aggregated dashboard metrics for the authenticated user.")
public record DashboardSummaryModel(
        @Schema(description = "Total number of books in the catalog.", example = "250")
        long totalBooks,
        @Schema(description = "Number of loans requested during the selected period.", example = "18")
        long monthlyLoans,
        @Schema(description = "Number of overdue loans belonging to the authenticated user.", example = "2")
        long overdueLoans
) {}
