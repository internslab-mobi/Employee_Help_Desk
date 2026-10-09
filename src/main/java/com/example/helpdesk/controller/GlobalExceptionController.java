package com.example.helpdesk.controller;

import com.example.helpdesk.cache.ErrorCodeCache;
import com.example.helpdesk.dto.response.ErrorResponseDTO;
import com.example.helpdesk.entity.ErrorCode;
import com.example.helpdesk.exception.AuthenticationException;
import com.example.helpdesk.exception.AuthorizationException;
import com.example.helpdesk.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionController.class);

    private final ErrorCodeCache errorCodeCache;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        String code = ex.getErrorCode();
        ErrorCode errorCode = errorCodeCache.getErrorCode(code);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        log.error("Authentication failed: {}", ex.getMessage());
        String code = ex.getErrorCode();
        ErrorCode errorCode = errorCodeCache.getErrorCode(code);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthorizationException(
            AuthorizationException ex,
            HttpServletRequest request) {
        log.error("Authorization failed: {}", ex.getMessage());
        String code = ex.getErrorCode();
        ErrorCode errorCode = errorCodeCache.getErrorCode(code);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        ErrorCode errorCode = errorCodeCache.getErrorCode("ERR_004");
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage("Invalid email or password")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        ErrorCode errorCode = errorCodeCache.getErrorCode("ERR_005");
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage("You do not have permission to access this resource")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation failed: {}", errorMessage);
        ErrorCode errorCode = errorCodeCache.getErrorCode("ERR_002");
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage("Validation failed: " + errorMessage)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.error("Invalid argument: {}", ex.getMessage());
        ErrorCode errorCode = errorCodeCache.getErrorCode("ERR_006");
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage(ex.getMessage())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }

    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbortException(
            ClientAbortException ex,
            HttpServletRequest request) {
        log.debug("Client aborted connection: {}", ex.getMessage());
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorCode errorCode = errorCodeCache.getErrorCode("ERR_010");
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .errorCode(errorCode.getCode())
                .statusCode(errorCode.getHttpStatus())
                .errorMessage("An unexpected error occurred")
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.valueOf(errorCode.getHttpStatus())).body(error);
    }
}




