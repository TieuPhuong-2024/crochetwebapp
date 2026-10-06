package org.crochet.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.payload.response.ResponseData;
import org.crochet.util.ResponseUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseData<Object> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failure at [{}]: {}", request.getRequestURI(), errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseData<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Constraint violation at [{}]: {}", request.getRequestURI(), errors);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Invalid input data", errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseData<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch at [{}]: {}", request.getRequestURI(), message);
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseData<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseData<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({java.lang.IllegalArgumentException.class, java.lang.IllegalStateException.class})
    public ResponseData<Object> handleIllegalArgumentOrState(RuntimeException ex, HttpServletRequest request) {
        log.warn("Invalid argument/state at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ResponseData<Object> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.BAD_REQUEST.value();
        log.warn("Bad request at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseData<Object> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.UNAUTHORIZED.value();
        log.warn("Unauthorized access at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseData<Object> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(TokenException.class)
    public ResponseData<Object> handleTokenException(TokenException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.UNAUTHORIZED.value();
        log.warn("Token exception at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(OAuth2AuthenticationProcessingException.class)
    public ResponseData<Object> handleOAuth2AuthenticationProcessingException(
            OAuth2AuthenticationProcessingException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.UNAUTHORIZED.value();
        log.warn("OAuth2 error at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseData<Object> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.FORBIDDEN.value();
        log.warn("Forbidden access at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseData<Object> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(HttpStatus.FORBIDDEN, "Access Denied: " + ex.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseData<Object> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.NOT_FOUND.value();
        log.warn("Resource not found at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage());
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseData<Object> handleUsernameNotFoundException(UsernameNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found at [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseUtil.error(ResultCode.MSG_USER_NOT_FOUND.code(), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseData<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseUtil.error(HttpStatus.CONFLICT, "Data integrity violation or duplicate resource");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(StorageException.class)
    public ResponseData<Object> handleStorageException(StorageException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        log.error("Storage error at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage(), ex);
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseData<Object> handleEmailVerificationException(EmailVerificationException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        log.error("Email verification error at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage(), ex);
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(EmailException.class)
    public ResponseData<Object> handleEmailException(EmailException ex, HttpServletRequest request) {
        int code = ex.getMessageCode() != 0 ? ex.getMessageCode() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        log.error("Email service error at [{}] - code {}: {}", request.getRequestURI(), code, ex.getMessage(), ex);
        return ResponseUtil.error(code, ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseData<Object> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error processing [{}]", request.getRequestURI(), ex);
        return ResponseUtil.error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }
}
