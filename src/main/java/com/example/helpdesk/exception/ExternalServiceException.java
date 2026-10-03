package com.example.helpdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an external service fails.
 * Maps to ERP_009 with HTTP 502.
 */
public class ExternalServiceException extends ApplicationException {

    public ExternalServiceException(String message) {
        super("ERP_009", message, HttpStatus.BAD_GATEWAY);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super("ERP_009", message, HttpStatus.BAD_GATEWAY, cause);
    }
}






