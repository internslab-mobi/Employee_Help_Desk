package com.divya.helpdesk.exception;

public class TicketNotFoundException extends ResourceNotFoundException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
