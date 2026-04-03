package com.medicalstorage.exception;

import com.medicalstorage.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP mapping.
 * All controllers propagate exceptions here; no try/catch boilerplate needed.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 404 ───────────────────────────────────────────────────
    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ImageNotFoundException ex, HttpServletRequest req) {
        log.warn("Image not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    // ── 400 – Business validation ─────────────────────────────
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(
            InvalidFileException ex, HttpServletRequest req) {
        log.warn("Invalid file: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_FILE", ex.getMessage(), req.getRequestURI());
    }

    // ── 400 – Bean Validation (@Valid) ────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation error: {}", details);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", details, req.getRequestURI());
    }

    // ── 413 – File too large ──────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Uploaded file exceeds the maximum allowed size (100 MB)", req.getRequestURI());
    }

    // ── 502 – SeaweedFS failure ───────────────────────────────
    @ExceptionHandler(SeaweedFSException.class)
    public ResponseEntity<ErrorResponse> handleSeaweedFS(
            SeaweedFSException ex, HttpServletRequest req) {
        log.error("SeaweedFS error: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", ex.getMessage(), req.getRequestURI());
    }

    // ── 500 – Catch-all ───────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support.", req.getRequestURI());
    }

    // ── Helper ────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, String path) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
