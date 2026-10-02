package com.divya.helpdesk.exception;

import com.divya.helpdesk.service.ErrorCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorCodeService errorCodeService;

    private ResponseEntity<ErrorResponse> createErrorResponse(String exceptionType, HttpStatus status, String customMessage) {
        String code = errorCodeService.getErrorCode(exceptionType);
        String finalMessage = (customMessage != null && !customMessage.isBlank()) ? customMessage : errorCodeService.getErrorMessage(code);
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
    @ExceptionHandler({
            TicketNotFoundException.class,
            UserNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return createErrorResponse("ResourceNotFoundException", HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 BAD_REQUEST - ERR_003 / ERR_006 / ERR_007 / ERR_008
    @ExceptionHandler({
            InvalidOperationException.class,
            BadRequestException.class,
            IllegalArgumentException.class //
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return createErrorResponse("InvalidOperationException", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 403 FORBIDDEN - ERR_004
    @ExceptionHandler({
            AccessDeniedException.class,
            UnauthorizedActionException.class
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return createErrorResponse("AccessDeniedException", HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 403 FORBIDDEN - ERR_005
    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActivated(AccountNotActivatedException ex) {
        return createErrorResponse("AccountNotActivatedException", HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 401 UNAUTHORIZED - ERR_004
    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {

        return createErrorResponse("BadCredentialsException", HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 400 BAD_REQUEST - Invalid OTP
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex) {
        return createErrorResponse("InvalidOtpException", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 BAD_REQUEST - Password Mismatch
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex) {
        return createErrorResponse("PasswordMismatchException", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 BAD_REQUEST / 422 - Validation Errors
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return createErrorResponse("ValidationException", HttpStatus.BAD_REQUEST, ex.getMessage());
    }


    @ExceptionHandler(MaxUploadSizeException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeException ex) {
        return createErrorResponse("ValidationException", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 409 CONFLICT - ERR_010
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        String message = ex != null ? ex.getMessage() : "A duplicate record or constraint violation occurred";
        return createErrorResponse("DuplicateResourceException", HttpStatus.CONFLICT, message);
    }

    // Base Application Business Exception Handler
    @ExceptionHandler(HDBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(HDBusinessException ex) {
        return createErrorResponse(ex.getClass().getSimpleName().toUpperCase(), HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 500 INTERNAL_SERVER_ERROR - ERR_999 (Do not expose SQL, stack traces, or credentials)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        return createErrorResponse("INTERNALSERVEREXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.");
    }
}
