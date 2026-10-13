package xyz.mobi.Employee_Help_Desk_Auth.exception;

public class HelpdeskException extends RuntimeException {

    public HelpdeskException(String message) {
        super(message);
    }

    public HelpdeskException(String message, Throwable cause) {
        super(message, cause);
    }
}
