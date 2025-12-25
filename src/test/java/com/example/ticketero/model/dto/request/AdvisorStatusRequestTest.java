package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.AdvisorStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorStatusRequest DTO Tests")
class AdvisorStatusRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Constructor y Propiedades")
    class ConstructorYPropiedades {

        @Test
        @DisplayName("Debe crear AdvisorStatusRequest válido")
        void debeCrearAdvisorStatusRequestValido() {
            // When
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.BUSY,
                "Atendiendo cliente prioritario"
            );

            // Then
            assertThat(request.status()).isEqualTo(AdvisorStatus.BUSY);
            assertThat(request.reason()).isEqualTo("Atendiendo cliente prioritario");
        }

        @Test
        @DisplayName("Debe crear AdvisorStatusRequest sin razón")
        void debeCrearAdvisorStatusRequestSinRazon() {
            // When
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.AVAILABLE,
                null
            );

            // Then
            assertThat(request.status()).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(request.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("Debe validar AdvisorStatusRequest correcto")
        void debeValidarAdvisorStatusRequestCorrecto() {
            // Given
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.OFFLINE,
                "Fin de turno"
            );

            // When
            Set<ConstraintViolation<AdvisorStatusRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Debe fallar con status null")
        void debeFallarConStatusNull() {
            // Given
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                null,
                "Alguna razón"
            );

            // When
            Set<ConstraintViolation<AdvisorStatusRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Estado es obligatorio");
        }

        @Test
        @DisplayName("Debe fallar con razón muy larga")
        void debeFallarConRazonMuyLarga() {
            // Given
            String razonLarga = "A".repeat(201);
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.BUSY,
                razonLarga
            );

            // When
            Set<ConstraintViolation<AdvisorStatusRequest>> violations = validator.validate(request);

            // Then
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Razón máximo 200 caracteres");
        }
    }
}