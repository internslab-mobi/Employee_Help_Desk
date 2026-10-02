package com.divya.helpdesk.exception;

public class HDBusinessException extends RuntimeException {

    public HDBusinessException(String message) {
        super(message);
    }

    public HDBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
