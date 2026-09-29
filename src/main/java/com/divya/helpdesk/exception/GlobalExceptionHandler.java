package com.divya.helpdesk.exception;

import com.divya.helpdesk.service.HDErrorCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final HDErrorCodeService errorCodeService;

    private ResponseEntity<ErrorResponse> createErrorResponse(String exceptionType, HttpStatus status, String customMessage) {
        String code = errorCodeService.getErrorCode(exceptionType);
        String finalMessage = (customMessage != null && !customMessage.isBlank())
                ? customMessage : errorCodeService.getErrorMessage(code);
        OffsetDateTime timestamp = OffsetDateTime.now(ZoneId.systemDefault());

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(code)
                .errorMessage(finalMessage)
                .statusCode(status.value())
                .timestamp(timestamp)
                .build();

        return new ResponseEntity<>(response, status);
    }

    // 404 NOT_FOUND - ERR_001
    @ExceptionHandler({TicketNotFoundException.class, UserNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return createErrorResponse("RESOURCENOTFOUNDEXCEPTION", HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 BAD_REQUEST - ERR_003 / ERR_006 / ERR_007 / ERR_008
    @ExceptionHandler({InvalidOperationException.class, BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return createErrorResponse("INVALIDOPERATIONEXCEPTION", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 403 FORBIDDEN - ERR_004
    @ExceptionHandler({org.springframework.security.access.AccessDeniedException.class, com.divya.helpdesk.exception.AccessDeniedException.class, UnauthorizedActionException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException ex) {
        return createErrorResponse("ACCESSDENIEDEXCEPTION", HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ERR_005
    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActivated(AccountNotActivatedException ex) {

        return createErrorResponse("ACCOUNTNOTACTIVATEDEXCEPTION", HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 401 UNAUTHORIZED - ERR_004
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {

        return createErrorResponse("BADCREDENTIALSEXCEPTION", HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex) {

        return createErrorResponse("INVALIDOTPEXCEPTION", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex) {

        return createErrorResponse("PASSWORDMISMATCHEXCEPTION", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {

        return createErrorResponse("VALIDATIONEXCEPTION", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return createErrorResponse("VALIDATIONEXCEPTION", HttpStatus.BAD_REQUEST, validationErrors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {

        return createErrorResponse("VALIDATIONEXCEPTION", HttpStatus.BAD_REQUEST, "File upload exceeds the maximum permitted size of 10MB");
    }

    // 409 CONFLICT - ERR_010
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        String message = ex != null ? ex.getMessage() : "A duplicate record or constraint violation occurred";

        return createErrorResponse("DUPLICATERESOURCEEXCEPTION", HttpStatus.CONFLICT, message);
    }

    // 500 INTERNAL_SERVER_ERROR - ERR_999 (Do not expose SQL, stack traces, or credentials)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);

        return createErrorResponse("INTERNALSERVEREXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.");
    }
}

