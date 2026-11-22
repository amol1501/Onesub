package com.onesub.exception;

import com.onesub.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                false, "Validation failed", "VALIDATION_ERROR", errors, Instant.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                false, ex.getMessage(), "VALIDATION_ERROR", Map.of(), Instant.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                false, ex.getMessage(), "NOT_FOUND", Map.of(), Instant.now()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                false, ex.getMessage(), "DUPLICATE_RESOURCE", Map.of(), Instant.now()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                false, ex.getMessage(), "FORBIDDEN", Map.of(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                false, ex.getMessage(), "INVALID_REQUEST", Map.of(), Instant.now()));
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
                false, "Invalid email or password", "UNAUTHORIZED", Map.of(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                false, "An unexpected error occurred", "INTERNAL_ERROR", Map.of(), Instant.now()));
    }
}
