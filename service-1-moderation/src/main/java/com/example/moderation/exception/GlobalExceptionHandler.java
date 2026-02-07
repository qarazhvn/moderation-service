package com.example.moderation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, 
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid"));
        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(Map.of(
                "status", "VALIDATION_ERROR", "message", "Validation failed", 
                "errors", fieldErrors, "timestamp", LocalDateTime.now()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "ERROR", "message", "Unexpected error", 
                "error", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }
    
    @ExceptionHandler(EnrichmentServiceException.class)
    public ResponseEntity<Map<String, Object>> handleEnrichmentServiceError(EnrichmentServiceException ex) {
        log.error("Enrichment error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "ENRICHMENT_ERROR", "message", "Failed to get enrichment data",
                "error", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }
    
    public static class EnrichmentServiceException extends RuntimeException {
        public EnrichmentServiceException(String message) { super(message); }
        public EnrichmentServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
