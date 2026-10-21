package xyz.mobi.Employee_Help_Desk_Auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends HelpdeskException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
