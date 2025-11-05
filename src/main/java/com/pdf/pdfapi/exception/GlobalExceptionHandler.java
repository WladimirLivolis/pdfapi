package com.pdf.pdfapi.exception;

import com.pdf.pdfapi.dto.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    private static final String ERROR_STATUS = "error";

    @ExceptionHandler(PdfErrorException.class)
    public ResponseEntity<ErrorResponse> handlePdfError(PdfErrorException ex, HttpServletRequest request) {
        log.error("PDF operation error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message(ex.getMessage())
                .error("PDF_OPERATION_ERROR")
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Validation error: {}", errors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message("Validation failed")
                .error("VALIDATION_ERROR")
                .details(errors)
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.error("File upload size exceeded: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message("File size exceeds maximum allowed size (100MB)")
                .error("FILE_SIZE_EXCEEDED")
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message(ex.getMessage())
                .error("INVALID_ARGUMENT")
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded for request: {}", request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message("Too many requests. Please try again later.")
                .error("RATE_LIMIT_EXCEEDED")
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ERROR_STATUS)
                .message("An unexpected error occurred")
                .error("INTERNAL_SERVER_ERROR")
                .path(request.getRequestURI())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
