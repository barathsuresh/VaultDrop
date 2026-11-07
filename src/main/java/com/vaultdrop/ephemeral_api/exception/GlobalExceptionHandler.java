package com.vaultdrop.ephemeral_api.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPassphraseException.class)
    public ResponseEntity<Object> handleAuthFailure(InvalidPassphraseException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationFailure(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, msg != null ? msg : "Malformed payload");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, "Secret not found");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleMalformedUuid(IllegalArgumentException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, "Secret not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtExceptions(Exception ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred.");
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, HttpStatus status, String customMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", customMessage);
        body.put("details", ex.getMessage()); // Optional: safe to omit or scrub in production environments

        // Optional: Differentiate details dynamically if needed
        if (ex instanceof HandlerMethodValidationException) {
            body.put("message", "Method parameter validation failed.");
        }

        return new ResponseEntity<>(body, status);
    }
}
