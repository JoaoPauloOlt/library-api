package com.jpoltramari.library_api.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Standardized API error response. Sensitive exception details may be omitted in production.")
public class ErrorResponse {

    @Schema(description = "Date and time when the error was generated.", example = "2026-09-16T15:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code.", example = "404")
    private Integer status;

    @Schema(description = "Short error category.", example = "Resource Not Found")
    private String title;

    @Schema(description = "Human-readable error detail. Production configuration may omit sensitive details.", example = "Book not found.")
    private String detail;

    @Schema(description = "Request path that produced the error.", example = "/books/999")
    private String path;

    @Schema(description = "Correlation identifier used to trace the request in application logs.", example = "7c2f5f4d-2a9e-4f2d-8f3a-7e3d4a2a5f11")
    private String correlationId;

    @Schema(description = "Stable application-specific error code.", example = "BOOK_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Exception type associated with the error when exposed by the application configuration.", example = "BookNotFoundException")
    private String exceptionType;

}
