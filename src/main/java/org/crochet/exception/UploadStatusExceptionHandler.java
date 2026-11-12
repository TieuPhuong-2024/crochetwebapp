package org.crochet.exception;

import org.crochet.payload.response.ResponseData;
import org.crochet.util.ResponseUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Keeps upload/domain lifecycle statuses from being caught by the generic 500 handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadStatusExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseData<Object>> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ResponseUtil.error(
                exception.getStatusCode().value(), exception.getReason() == null ? "Upload request failed" : exception.getReason()));
    }
}
