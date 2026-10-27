package com.felixcasa.backendjava.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequestBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", "Invalid request body."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException ex) {
        return new ResponseEntity<>("Resource not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("detail", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", "Invalid request fields"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Image or repository already exists"));
    }

    @ExceptionHandler(org.springframework.jdbc.UncategorizedSQLException.class)
    public ResponseEntity<Map<String, String>> handleSql(org.springframework.jdbc.UncategorizedSQLException ex) {
        int code = ex.getSQLException().getErrorCode();
        if (code == 19) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Image or repository already exists"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", "Database operation failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        logger.error("Unhandled exception occurred", ex);
        return new ResponseEntity<>("An internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(RepoNotFoundException.class)
    public ResponseEntity<String> handleRepoNotFound(RepoNotFoundException ex) {
        logger.warn("Repo not found: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<String> handleTemplateNotFound(TemplateNotFoundException ex) {
        logger.warn("Template not found: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}
