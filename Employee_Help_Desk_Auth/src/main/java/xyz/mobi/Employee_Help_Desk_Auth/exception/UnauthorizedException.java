package xyz.mobi.Employee_Help_Desk_Auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends HelpdeskException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
