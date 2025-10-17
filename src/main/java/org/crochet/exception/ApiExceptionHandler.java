package org.crochet.exception;

import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.payload.response.ResponseData;
import org.crochet.util.ResponseUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@ResponseBody
public class ApiExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ResponseData<String>> handleException(Exception ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var resultCode = ResultCode.INTERNAL_SERVER_ERROR;
        var error = ResponseData.<String>builder()
                .success(false)
                .code(resultCode.code())
                .message(ex.getMessage())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(error, resultCode.status());
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ResponseData<String>> handleAuthenticationException(AuthenticationException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var resultCode = ResultCode.UNAUTHORIZED_ERROR;
        var error = ResponseUtil.error(resultCode.code(), ex.getMessage(), ex.getCause());
        return new ResponseEntity<>(error, resultCode.status());
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<ResponseData<String>> handleBadRequestException(BadRequestException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var err = ResponseData.<String>builder()
                .success(false)
                .code(ex.getMessageCode())
                .message(ex.getMessage())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(err, ResultCode.fromCode(ex.getMessageCode()).status());
    }

    @ExceptionHandler({OAuth2AuthenticationProcessingException.class})
    public ResponseEntity<ResponseData<String>> handleOAuth2AuthenticationProcessingException(
            OAuth2AuthenticationProcessingException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var err = ResponseData.<String>builder()
                .success(false)
                .code(ex.getMessageCode())
                .message(ex.getMessage())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(err, ResultCode.fromCode(ex.getMessageCode()).status());
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    public ResponseEntity<ResponseData<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var err = ResponseData.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .code(ex.getMessageCode())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(err, ResultCode.fromCode(ex.getMessageCode()).status());
    }

    @ExceptionHandler({EmailVerificationException.class})
    public ResponseEntity<ResponseData<String>> handleEmailVerificationException(EmailVerificationException ex) {
        return new ResponseEntity<>(handleInternalError(ex, ex.getMessageCode()), ResultCode.fromCode(ex.getMessageCode()).status());
    }

    @ExceptionHandler({TokenException.class})
    public ResponseEntity<ResponseData<String>> handleTokenException(TokenException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var err = ResponseData.<String>builder()
                .success(false)
                .code(ex.getMessageCode())
                .message(ex.getMessage())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(err, ResultCode.fromCode(ex.getMessageCode()).status());
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    public ResponseEntity<ResponseData<String>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var resultCode = ResultCode.MSG_USER_NOT_FOUND;
        return new ResponseEntity<>(ResponseUtil.error(resultCode.code(), ex.getMessage(), ex.getCause()), resultCode.status());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseData<String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        log.error(ex.getMessage());
        log.error(ex.toString());
        var resultCode = ResultCode.DATA_INTEGRITY_VIOLATION;
        return new ResponseEntity<>(ResponseUtil.error(resultCode.code(), message, ex.getCause()), resultCode.status());
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ResponseData<String>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        var err = ResponseData.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .code(ex.getMessageCode())
                .error(ex.getCause())
                .build();
        return new ResponseEntity<>(err, ResultCode.fromCode(ex.getMessageCode()).status());
    }

    private ResponseData<String> handleInternalError(RuntimeException ex, int messageCode) {
        log.error(ex.getMessage());
        log.error(ex.toString());
        return ResponseUtil.error(messageCode, ex.getMessage(), ex.getCause());
    }
}
