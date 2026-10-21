package xyz.mobi.employeehelpdesk.exception;

public class HelpdeskException extends RuntimeException {

    public HelpdeskException(String message) {
        super(message);
    }

    public HelpdeskException(String message, Throwable cause) {
        super(message, cause);
    }
}