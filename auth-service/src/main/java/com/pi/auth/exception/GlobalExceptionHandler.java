package com.pi.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthException ex) {
        HttpStatus status;
        switch (ex.getCode()) {
            case "EMAIL_ALREADY_REGISTERED" -> status = HttpStatus.CONFLICT;
            case "ACCOUNT_NOT_ACTIVE", "ACCOUNT_DISABLED" -> status = HttpStatus.FORBIDDEN;
            case "ACCOUNT_LOCKED" -> status = HttpStatus.LOCKED;
            case "INVALID_CREDENTIALS" -> status = HttpStatus.UNAUTHORIZED;
            default -> status = HttpStatus.BAD_REQUEST;
        }
        ApiError error = new ApiError(Instant.now(), status.value(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidTokenException(InvalidTokenException ex) {
        ApiError error = new ApiError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "TOKEN_INVALID", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        ApiError error = new ApiError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        String code = ex.getMessage() != null && ex.getMessage().contains("Email") ? "EMAIL_ALREADY_REGISTERED" : "BAD_REQUEST";
        HttpStatus status = "Invalid credentials".equalsIgnoreCase(ex.getMessage()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        if (status == HttpStatus.UNAUTHORIZED) {
            code = "INVALID_CREDENTIALS";
        }
        ApiError error = new ApiError(Instant.now(), status.value(), code, ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
        String code = "ACCOUNT_NOT_ACTIVE";
        if (ex.getMessage() != null && ex.getMessage().contains("locked")) {
            code = "ACCOUNT_LOCKED";
        }
        ApiError error = new ApiError(Instant.now(), HttpStatus.FORBIDDEN.value(), code, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                details.isEmpty() ? "Validation failed" : details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
