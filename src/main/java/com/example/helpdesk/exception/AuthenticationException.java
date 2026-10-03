package com.example.helpdesk.exception;

public class AuthenticationException extends RuntimeException {

    private final String errorCode;

    public AuthenticationException(String message) {
        this("ERR_004", message);
    }

    public AuthenticationException(String message, Throwable cause) {
        this("ERR_004", message, cause);
    }

    public AuthenticationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

