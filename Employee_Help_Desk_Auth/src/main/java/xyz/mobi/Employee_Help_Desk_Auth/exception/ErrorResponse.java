package xyz.mobi.Employee_Help_Desk_Auth.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorCode,
        String message,
        int statusCode,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String errorCode, int statusCode, String message) {
        return new ErrorResponse(errorCode, message, statusCode, LocalDateTime.now());
    }
}
