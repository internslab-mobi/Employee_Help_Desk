package com.example.helpdesk.exception;

/**
 * Exception thrown when an authenticated user does not have permission to access a resource.
 * This exception carries an error code that maps to the hd_error_codes table for consistent error responses.
 */
public class AuthorizationException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructor with default error code ERR_005 (Access denied).
     *
     * @param message the error message
     */
    public AuthorizationException(String message) {
        this("ERR_005", message);
    }

    /**
     * Constructor with default error code ERR_005 (Access denied) and underlying cause.
     *
     * @param message the error message
     * @param cause   the underlying cause of the exception
     */
    public AuthorizationException(String message, Throwable cause) {
        this("ERR_005", message, cause);
    }

    /**
     * Constructor with custom error code.
     *
     * @param errorCode the error code (e.g., ERR_005, ERR_011)
     * @param message   the error message
     */
    public AuthorizationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with custom error code and underlying cause.
     *
     * @param errorCode the error code (e.g., ERR_005, ERR_011)
     * @param message   the error message
     * @param cause     the underlying cause of the exception
     */
    public AuthorizationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code (e.g., ERR_005)
     */
    public String getErrorCode() {
        return errorCode;
    }
}
