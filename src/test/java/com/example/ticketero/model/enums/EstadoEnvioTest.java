package com.example.ticketero.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EstadoEnvio Enum Tests")
class EstadoEnvioTest {

    @Nested
    @DisplayName("Valores del Enum")
    class ValoresDelEnum {

        @Test
        @DisplayName("Debe tener todos los valores esperados")
        void debeTenerTodosLosValoresEsperados() {
            // When
            EstadoEnvio[] values = EstadoEnvio.values();

            // Then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactly(
                EstadoEnvio.PENDIENTE,
                EstadoEnvio.ENVIADO,
                EstadoEnvio.FALLIDO
            );
        }
    }

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("PENDIENTE debe tener descripción correcta")
        void pendienteDebeTenerDescripcionCorrecta() {
            // When
            EstadoEnvio estado = EstadoEnvio.PENDIENTE;

            // Then
            assertThat(estado.getDescription()).isEqualTo("Pendiente de envío");
        }

        @Test
        @DisplayName("ENVIADO debe tener descripción correcta")
        void enviadoDebeTenerDescripcionCorrecta() {
            // When
            EstadoEnvio estado = EstadoEnvio.ENVIADO;

            // Then
            assertThat(estado.getDescription()).isEqualTo("Enviado exitosamente");
        }

        @Test
        @DisplayName("FALLIDO debe tener descripción correcta")
        void fallidoDebeTenerDescripcionCorrecta() {
            // When
            EstadoEnvio estado = EstadoEnvio.FALLIDO;

            // Then
            assertThat(estado.getDescription()).isEqualTo("Falló después de reintentos");
        }
    }
}