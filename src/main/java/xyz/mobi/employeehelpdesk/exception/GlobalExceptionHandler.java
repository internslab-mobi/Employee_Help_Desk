package xyz.mobi.employeehelpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(
            BadRequestException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(
            IllegalStateException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse(
                        HttpStatus.CONFLICT,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<?> handleHolidayNotFound(
            HolidayNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(
            UnauthorizedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse(
                        HttpStatus.UNAUTHORIZED,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex
    ) {
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Access is denied";
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse(
                        HttpStatus.FORBIDDEN,
                        msg
                ));
    }

    private Map<String, Object> errorResponse(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        return response;
    }
}