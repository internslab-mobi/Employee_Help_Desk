package com.example.helpdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an unexpected internal error occurs.
 * Maps to ERP_010 with HTTP 500.
 */
public class InternalApplicationException extends ApplicationException {

    public InternalApplicationException(String message) {
        super("ERP_010", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalApplicationException(String message, Throwable cause) {
        super("ERP_010", message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}





