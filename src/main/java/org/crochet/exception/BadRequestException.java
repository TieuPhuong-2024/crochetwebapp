package org.crochet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends DecoratedRuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message, int messageCode) {
        super(message, messageCode);
    }

    public BadRequestException(String message, Throwable cause, int messageCode) {
        super(message, cause, messageCode);
    }
}
