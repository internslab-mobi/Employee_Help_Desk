package xyz.mobi.notificationservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
