package xyz.mobi.employeehelpdesk.exception;

public class ServiceUnavailableException extends HelpdeskException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
