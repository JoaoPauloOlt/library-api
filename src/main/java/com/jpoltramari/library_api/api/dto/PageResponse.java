package com.jpoltramari.library_api.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(name = "PageResponse", description = "Standard paginated response returned by collection endpoints.")
public record PageResponse<T>(
        @Schema(description = "Records contained in the current page.")
        List<T> content,
        @Schema(description = "Zero-based page number.", example = "0")
        int page,
        @Schema(description = "Number of records requested per page.", example = "20")
        int size,
        @Schema(description = "Total number of records across all pages.", example = "42")
        long totalElements,
        @Schema(description = "Total number of available pages.", example = "3")
        int totalPages,
        @Schema(description = "Whether this is the first page.", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page.", example = "false")
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
