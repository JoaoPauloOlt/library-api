package com.jpoltramari.library_api.api.exception;

import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.infrastructure.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ErrorProperties properties = new ErrorProperties();
        properties.setExposeDetails(false);
        handler = new ApiExceptionHandler(properties);

        request = new MockHttpServletRequest();
        request.setRequestURI("/books/999");
    }

    @Test
    void shouldReturnNotFoundResponseForMissingResource() {
        EntityNotFoundException exception = new EntityNotFoundException("Book 999 not found");

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Resource not found");
        assertThat(response.getBody().getDetail()).isEqualTo("Book 999 not found");
        assertThat(response.getBody().getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getPath()).isEqualTo("/books/999");
    }

    @Test
    void shouldReturnBadRequestForBusinessRuleViolation() {
        BusinessException exception = new BusinessException("Book has no available copies");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Business rule violation");
        assertThat(response.getBody().getDetail()).isEqualTo("Book has no available copies");
        assertThat(response.getBody().getErrorCode()).isEqualTo("BUSINESS_RULE");
    }

    @Test
    void shouldHideInternalDetailsWhenDetailsAreDisabled() {
        IllegalStateException exception = new IllegalStateException("Sensitive internal implementation detail");

        ResponseEntity<ErrorResponse> response = handler.handleUncaught(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getExceptionType()).isNull();
    }

    @Test
    void shouldExposeDetailsOnlyWhenExplicitlyEnabled() {
        ErrorProperties properties = new ErrorProperties();
        properties.setExposeDetails(true);
        ApiExceptionHandler configuredHandler = new ApiExceptionHandler(properties);
        IllegalStateException exception = new IllegalStateException("Expected diagnostic detail");

        ResponseEntity<ErrorResponse> response = configuredHandler.handleUncaught(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Expected diagnostic detail");
        assertThat(response.getBody().getExceptionType()).isEqualTo("IllegalStateException");
    }
}
