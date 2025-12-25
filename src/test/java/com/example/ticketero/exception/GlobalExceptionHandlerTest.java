package com.example.ticketero.exception;

import com.example.ticketero.model.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @Nested
    @DisplayName("handleValidation()")
    class HandleValidation {

        @Test
        @DisplayName("debe manejar errores de validación correctamente")
        void handleValidation_debeRetornarErrorResponse() {
            // Given
            FieldError fieldError1 = new FieldError("ticketRequest", "rut", "RUT is required");
            FieldError fieldError2 = new FieldError("ticketRequest", "telefono", "Invalid phone format");
            
            when(validationException.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(validationException);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Validation failed");
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().errors()).containsExactly(
                "rut: RUT is required",
                "telefono: Invalid phone format"
            );
        }
    }

    @Nested
    @DisplayName("handleTicketNotFound()")
    class HandleTicketNotFound {

        @Test
        @DisplayName("debe manejar TicketNotFoundException correctamente")
        void handleTicketNotFound_debeRetornar404() {
            // Given
            TicketNotFoundException exception = new TicketNotFoundException("C01");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleTicketNotFound(exception);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("C01");
            assertThat(response.getBody().status()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("handleActiveTicketExists()")
    class HandleActiveTicketExists {

        @Test
        @DisplayName("debe manejar ActiveTicketExistsException correctamente")
        void handleActiveTicketExists_debeRetornar409() {
            // Given
            ActiveTicketExistsException exception = new ActiveTicketExistsException("12345678-9", "C01");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleActiveTicketExists(exception);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("12345678-9");
            assertThat(response.getBody().status()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("handleBadRequest()")
    class HandleBadRequest {

        @Test
        @DisplayName("debe manejar IllegalArgumentException correctamente")
        void handleBadRequest_debeRetornar400() {
            // Given
            IllegalArgumentException exception = new IllegalArgumentException("Invalid queue type");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(exception);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Invalid queue type");
            assertThat(response.getBody().status()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("handleGeneral()")
    class HandleGeneral {

        @Test
        @DisplayName("debe manejar Exception genérica correctamente")
        void handleGeneral_debeRetornar500() {
            // Given
            RuntimeException exception = new RuntimeException("Database connection failed");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneral(exception);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Internal server error");
            assertThat(response.getBody().status()).isEqualTo(500);
        }
    }
}