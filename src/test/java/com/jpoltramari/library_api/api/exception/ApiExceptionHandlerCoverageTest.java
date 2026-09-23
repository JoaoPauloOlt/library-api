package com.jpoltramari.library_api.api.exception;

import com.jpoltramari.library_api.domain.exception.EntityInUseException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiExceptionHandlerCoverageTest {

    @Test
    void shouldHandleMalformedJson() {
        ApiExceptionHandler handler = handler(false);
        ResponseEntity<ErrorResponse> response = handler.handleJsonError(
                mock(HttpMessageNotReadableException.class), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("MALFORMED_JSON");
    }

    @Test
    void shouldHandleTypeMismatch() {
        ApiExceptionHandler handler = handler(false);
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("TYPE_MISMATCH");
        assertThat(response.getBody().getDetail()).contains("Parameter 'id'");
    }

    @Test
    void shouldHandleMissingParameter() {
        ApiExceptionHandler handler = handler(false);
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("page", "int");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParam(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("MISSING_PARAMETER");
    }

    @Test
    void shouldHandleEntityInUse() {
        ApiExceptionHandler handler = handler(false);

        ResponseEntity<ErrorResponse> response = handler.handleEntityInUse(
                new EntityInUseException("Author is in use"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ENTITY_IN_USE");
    }

    @Test
    void shouldHandleDataIntegrityViolation() {
        ApiExceptionHandler handler = handler(false);

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATA_INTEGRITY");
        assertThat(response.getBody().getDetail())
                .isEqualTo("Operation conflicts with existing data constraints");
    }

    @Test
    void shouldHandleDataAccessResourceUsage() {
        ApiExceptionHandler handler = handler(false);

        ResponseEntity<ErrorResponse> response = handler.handleDataAccess(
                new InvalidDataAccessResourceUsageException("database error"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATABASE_ERROR");
    }

    @Test
    void shouldHandleDataAccessApiUsage() {
        ApiExceptionHandler handler = handler(false);

        ResponseEntity<ErrorResponse> response = handler.handleDataAccess(
                new InvalidDataAccessApiUsageException("invalid query"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATABASE_ERROR");
    }

    private ApiExceptionHandler handler(boolean exposeDetails) {
        com.jpoltramari.library_api.infrastructure.config.ErrorProperties properties =
                new com.jpoltramari.library_api.infrastructure.config.ErrorProperties();
        properties.setExposeDetails(exposeDetails);
        return new ApiExceptionHandler(properties);
    }

    private HttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        return request;
    }
}
