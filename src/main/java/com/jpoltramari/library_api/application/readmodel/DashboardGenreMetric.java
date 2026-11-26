package com.jpoltramari.library_api.application.readmodel;

import com.jpoltramari.library_api.domain.enums.Genre;

public record DashboardGenreMetric(Genre genre, long loanCount) {
}
