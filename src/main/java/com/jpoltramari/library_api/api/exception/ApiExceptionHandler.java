package com.jpoltramari.library_api.api.exception;

import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityInUseException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.infrastructure.config.ErrorProperties;
import com.jpoltramari.library_api.infrastructure.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final ErrorProperties errorProperties;

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonError(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("event=bad_request reason=malformed_json path={} correlationId={}",
                request.getRequestURI(), correlationId());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON",
                resolveDetail(ex, "Request body is invalid or missing required fields"),
                "MALFORMED_JSON",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        log.warn("event=bad_request reason=validation_failed path={} correlationId={}",
                request.getRequestURI(), correlationId());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                detail,
                "VALIDATION_ERROR",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String detail = String.format(
                "Parameter '%s' with value '%s' could not be converted to type %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter",
                detail,
                "TYPE_MISMATCH",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing parameter",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                "MISSING_PARAMETER",
                request
        );
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(
            PropertyReferenceException ex,
            HttpServletRequest request
    ) {
        String typeName = ex.getType() != null
                ? ex.getType().getType().getSimpleName()
                : "unknown";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid sort or filter property",
                String.format("Unknown property '%s' for type %s", ex.getPropertyName(), typeName),
                "INVALID_PROPERTY",
                request
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage(),
                "NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(EntityInUseException.class)
    public ResponseEntity<ErrorResponse> handleEntityInUse(
            EntityInUseException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Entity in use",
                ex.getMessage(),
                "ENTITY_IN_USE",
                request
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Business rule violation",
                ex.getMessage(),
                "BUSINESS_RULE",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("event=conflict reason=data_integrity path={} correlationId={}",
                request.getRequestURI(), correlationId(), ex);

        return buildResponse(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                resolveDetail(ex, "Operation conflicts with existing data constraints"),
                "DATA_INTEGRITY",
                request
        );
    }

    @ExceptionHandler({
            InvalidDataAccessResourceUsageException.class,
            InvalidDataAccessApiUsageException.class
    })
    public ResponseEntity<ErrorResponse> handleDataAccess(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.error("event=database_error path={} correlationId={}",
                request.getRequestURI(), correlationId(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Database error",
                resolveDetail(ex, "A database operation failed"),
                "DATABASE_ERROR",
                request,
                ex
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUncaught(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("event=internal_error path={} correlationId={}",
                request.getRequestURI(), correlationId(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                resolveDetail(ex, "An unexpected error occurred"),
                "INTERNAL_ERROR",
                request,
                ex
        );
    }

    private String formatFieldError(FieldError error) {
        String message = error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "invalid value";
        return error.getField() + ": " + message;
    }

    private String resolveDetail(Exception ex, String fallback) {
        if (!errorProperties.isExposeDetails()) {
            return fallback;
        }
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null && !message.isBlank() ? message : fallback;
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request
    ) {
        return buildResponse(status, title, detail, errorCode, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request,
            Exception ex
    ) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .title(title)
                .detail(detail)
                .path(request.getRequestURI())
                .correlationId(correlationId())
                .errorCode(errorCode);

        if (errorProperties.isExposeDetails() && ex != null) {
            builder.exceptionType(ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(status).body(builder.build());
    }

    private static String correlationId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
