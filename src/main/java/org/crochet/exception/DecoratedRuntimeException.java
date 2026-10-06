package org.crochet.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecoratedRuntimeException extends RuntimeException {
    private int messageCode;

    public DecoratedRuntimeException(String message) {
        super(message);
    }

    public DecoratedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecoratedRuntimeException(String message, int messageCode) {
        super(message);
        this.messageCode = messageCode;
    }

    public DecoratedRuntimeException(String message, Throwable cause, int messageCode) {
        super(message, cause);
        this.messageCode = messageCode;
    }
}
