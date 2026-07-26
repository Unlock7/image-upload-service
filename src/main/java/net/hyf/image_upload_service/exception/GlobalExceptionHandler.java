package net.hyf.image_upload_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import net.hyf.image_upload_service.dto.error.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " +
                                error.getDefaultMessage()
                )
                .collect(Collectors.joining("; "));

        ErrorResponse response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = createErrorResponse(
                HttpStatus.CONFLICT,
                "Username or email already exists",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    private ErrorResponse createErrorResponse(
            HttpStatus status,
            String message,
            String path
    ) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                Instant.now()
        );
    }
}
