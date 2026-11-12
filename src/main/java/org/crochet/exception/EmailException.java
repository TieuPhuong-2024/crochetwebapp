package org.crochet.exception;

public class EmailException extends DecoratedRuntimeException {
    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailException(String message, int messageCode) {
        super(message, messageCode);
    }

    public EmailException(String message, Throwable cause, int messageCode) {
        super(message, cause, messageCode);
    }
}
