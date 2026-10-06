package org.crochet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends DecoratedRuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(String message, int messageCode) {
        super(message, messageCode);
    }

    public UnauthorizedException(String message, Throwable cause, int messageCode) {
        super(message, cause, messageCode);
    }
}
