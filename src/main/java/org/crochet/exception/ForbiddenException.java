package org.crochet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends DecoratedRuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(String message, int messageCode) {
        super(message, messageCode);
    }

    public ForbiddenException(String message, Throwable cause, int messageCode) {
        super(message, cause, messageCode);
    }
}
