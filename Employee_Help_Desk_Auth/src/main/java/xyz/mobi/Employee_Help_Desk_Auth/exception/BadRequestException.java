package xyz.mobi.Employee_Help_Desk_Auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends HelpdeskException {

    public BadRequestException(String message) {
        super(message);
    }
}
