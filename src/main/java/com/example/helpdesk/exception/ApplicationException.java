package com.example.helpdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all application-specific exceptions.
 * This provides a consistent structure for error handling across the application.
 */
public abstract class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public ApplicationException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApplicationException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}





