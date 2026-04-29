package com.jpoltramari.library_api.api.exception;

import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityInUseException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    public ResponseEntity<ErrorResponse> handleJsonError(
            HttpMessageNotReadableException ex, HttpServletRequest request){
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON",
                "Request body is invalid or incorrectly formated",
                request
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    @ExceptionHandler(EntityInUseException.class)
    public ResponseEntity<ErrorResponse> handleEntityInUse(
            EntityInUseException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "Entity in use", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Business rule violation", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "One or more fields are invalid: " + fields,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUncaught(
            Exception ex, HttpServletRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected internal system error has occurred. Try again later.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .title(title)
                .detail(detail)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}