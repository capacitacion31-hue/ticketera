package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TicketRequest DTO Tests")
class TicketRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Constructor y Propiedades")
    class ConstructorYPropiedades {

        @Test
        @DisplayName("Debe crear TicketRequest válido")
        void debeCrearTicketRequestValido() {
            // When
            TicketRequest request = new TicketRequest(
                "12345678",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // Then
            assertThat(request.nationalId()).isEqualTo("12345678");
            assertThat(request.telefono()).isEqualTo("+56912345678");
            assertThat(request.branchOffice()).isEqualTo("Sucursal Centro");
            assertThat(request.queueType()).isEqualTo(QueueType.CAJA);
        }

        @Test
        @DisplayName("Debe crear TicketRequest sin teléfono")
        void debeCrearTicketRequestSinTelefono() {
            // When
            TicketRequest request = new TicketRequest(
                "12345678",
                null,
                "Sucursal Centro",
                QueueType.PERSONAL_BANKER
            );

            // Then
            assertThat(request.nationalId()).isEqualTo("12345678");
            assertThat(request.telefono()).isNull();
            assertThat(request.branchOffice()).isEqualTo("Sucursal Centro");
            assertThat(request.queueType()).isEqualTo(QueueType.PERSONAL_BANKER);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("Debe validar TicketRequest correcto")
        void debeValidarTicketRequestCorrecto() {
            // Given
            TicketRequest request = new TicketRequest(
                "12345678",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // When
            Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Debe fallar con nationalId vacío")
        void debeFallarConNationalIdVacio() {
            // Given
            TicketRequest request = new TicketRequest(
                "",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // When
            Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(2);
            assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .containsAnyOf("El RUT/ID es obligatorio", "RUT/ID debe tener entre 8-20 caracteres");
        }

        @Test
        @DisplayName("Debe fallar con nationalId muy corto")
        void debeFallarConNationalIdMuyCorto() {
            // Given
            TicketRequest request = new TicketRequest(
                "123",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // When
            Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("RUT/ID debe tener entre 8-20 caracteres");
        }

        @Test
        @DisplayName("Debe fallar con teléfono inválido")
        void debeFallarConTelefonoInvalido() {
            // Given
            TicketRequest request = new TicketRequest(
                "12345678",
                "123456789",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // When
            Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Teléfono debe tener formato +56XXXXXXXXX");
        }

        @Test
        @DisplayName("Debe fallar con queueType null")
        void debeFallarConQueueTypeNull() {
            // Given
            TicketRequest request = new TicketRequest(
                "12345678",
                "+56912345678",
                "Sucursal Centro",
                null
            );

            // When
            Set<ConstraintViolation<TicketRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Tipo de cola es obligatorio");
        }
    }
}