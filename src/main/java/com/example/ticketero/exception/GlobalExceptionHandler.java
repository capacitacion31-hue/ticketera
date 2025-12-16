package com.example.ticketero.exception;

import com.example.ticketero.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();

        log.error("Validation errors: {}", errors);
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse("Validation failed", 400, errors));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException ex) {
        log.error("Ticket not found: {}", ex.getMessage());
        return ResponseEntity
            .status(404)
            .body(new ErrorResponse(ex.getMessage(), 404));
    }

    @ExceptionHandler(ActiveTicketExistsException.class)
    public ResponseEntity<ErrorResponse> handleActiveTicketExists(ActiveTicketExistsException ex) {
        log.error("Active ticket exists: {}", ex.getMessage());
        return ResponseEntity
            .status(409)
            .body(new ErrorResponse(ex.getMessage(), 409));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(ex.getMessage(), 400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
            .status(500)
            .body(new ErrorResponse("Internal server error", 500));
    }
}