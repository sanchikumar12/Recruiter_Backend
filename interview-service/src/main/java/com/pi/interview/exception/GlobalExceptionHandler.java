package com.pi.interview.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInterviewNotFound(InterviewNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SlotNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSlotNotFound(SlotNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "SLOT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InterviewerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInterviewerNotFound(InterviewerNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "INTERVIEWER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleSlotBooked(SlotAlreadyBookedException ex) {
        return buildResponse(HttpStatus.CONFLICT, "SLOT_ALREADY_BOOKED", ex.getMessage());
    }

    @ExceptionHandler(SlotExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleSlotExpired(SlotExpiredException ex) {
        return buildResponse(HttpStatus.CONFLICT, "SLOT_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(InterviewAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(InterviewAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "INTERVIEW_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(InterviewNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleNotAllowed(InterviewNotAllowedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INTERVIEW_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(InvalidInterviewStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidInterviewStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_INTERVIEW_STATE", ex.getMessage());
    }

    @ExceptionHandler(InvalidSlotException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSlot(InvalidSlotException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_SLOT", ex.getMessage());
    }

    @ExceptionHandler(InterviewerInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleInterviewerInactive(InterviewerInactiveException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INTERVIEWER_INACTIVE", ex.getMessage());
    }

    @ExceptionHandler(InterviewAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(InterviewAccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "INTERVIEW_ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("code", "VALIDATION_FAILED");
        response.put("message", "Input validation failed");
        response.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
