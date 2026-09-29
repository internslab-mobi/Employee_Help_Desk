package xyz.mobi.employeehelpdesk.exception;

/**
 * Thrown when creating a resource that conflicts with an existing one
 * (e.g. duplicate department code, holiday already exists for that date).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
