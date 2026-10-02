package com.divya.helpdesk.exception;

public class UnauthorizedActionException extends AccessDeniedException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
