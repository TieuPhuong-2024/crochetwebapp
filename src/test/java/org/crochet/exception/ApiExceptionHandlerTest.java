package org.crochet.exception;

import org.crochet.payload.response.ResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    void handleValidationExceptions_returnsBadRequestWithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("userDto", "email", "Email must be valid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseData<Object> response = handler.handleValidationExceptions(ex, request);

        assertFalse(response.isSuccess());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getCode());
        assertEquals("Validation failed", response.getMessage());
        assertNotNull(response.getErrors());
        assertEquals("Email must be valid", response.getErrors().get("email"));
    }

    @Test
    void handleDataIntegrityViolationException_returnsConflictAndHidesDbDetails() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Duplicate entry 'admin@example.com' for key 'uk_users_email'");

        ResponseData<Object> response = handler.handleDataIntegrityViolationException(ex, request);

        assertFalse(response.isSuccess());
        assertEquals(HttpStatus.CONFLICT.value(), response.getCode());
        assertEquals("Data integrity violation or duplicate resource", response.getMessage());
        assertNull(response.getErrors());
    }

    @Test
    void handleResourceNotFoundException_returnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Category not found", 40401);

        ResponseData<Object> response = handler.handleResourceNotFoundException(ex, request);

        assertFalse(response.isSuccess());
        assertEquals(40401, response.getCode());
        assertEquals("Category not found", response.getMessage());
    }

    @Test
    void handleForbiddenException_returnsForbidden() {
        ForbiddenException ex = new ForbiddenException("No permission", 40301);

        ResponseData<Object> response = handler.handleForbiddenException(ex, request);

        assertFalse(response.isSuccess());
        assertEquals(40301, response.getCode());
        assertEquals("No permission", response.getMessage());
    }

    @Test
    void handleUnexpectedException_returnsInternalServerErrorWithoutLeakingDetails() {
        RuntimeException ex = new RuntimeException("Sensitive database credentials leaked in internal message");

        ResponseData<Object> response = handler.handleUnexpectedException(ex, request);

        assertFalse(response.isSuccess());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getCode());
        assertEquals("An unexpected error occurred. Please try again later.", response.getMessage());
        assertNull(response.getErrors());
    }
}
