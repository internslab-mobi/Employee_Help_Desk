package com.example.helpdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when request validation fails.
 * Maps to ERP_002 with HTTP 400.
 */
public class ValidationException extends ApplicationException {

    public ValidationException(String message) {
        super("ERP_002", message, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, Throwable cause) {
        super("ERP_002", message, HttpStatus.BAD_REQUEST, cause);
    }
}





