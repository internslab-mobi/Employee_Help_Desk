package xyz.mobi.employeehelpdesk.exception;

/**
 * Thrown when a business operation violates a state invariant
 * (e.g. ticket already resolved, invalid status transition).
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}
