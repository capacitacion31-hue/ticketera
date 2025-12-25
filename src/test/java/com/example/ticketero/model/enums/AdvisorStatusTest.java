package com.example.ticketero.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorStatus Enum Tests")
class AdvisorStatusTest {

    @Nested
    @DisplayName("Valores del Enum")
    class ValoresDelEnum {

        @Test
        @DisplayName("Debe tener todos los valores esperados")
        void debeTenerTodosLosValoresEsperados() {
            // When
            AdvisorStatus[] values = AdvisorStatus.values();

            // Then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactly(
                AdvisorStatus.AVAILABLE,
                AdvisorStatus.BUSY,
                AdvisorStatus.OFFLINE
            );
        }

        @Test
        @DisplayName("Debe obtener valor por nombre")
        void debeObtenerValorPorNombre() {
            // When & Then
            assertThat(AdvisorStatus.valueOf("AVAILABLE")).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(AdvisorStatus.valueOf("BUSY")).isEqualTo(AdvisorStatus.BUSY);
            assertThat(AdvisorStatus.valueOf("OFFLINE")).isEqualTo(AdvisorStatus.OFFLINE);
        }
    }

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("AVAILABLE debe tener propiedades correctas")
        void availableDebeTenerPropiedadesCorrectas() {
            // When
            AdvisorStatus status = AdvisorStatus.AVAILABLE;

            // Then
            assertThat(status.getDescription()).isEqualTo("Disponible");
            assertThat(status.canReceiveAssignments()).isTrue();
        }

        @Test
        @DisplayName("BUSY debe tener propiedades correctas")
        void busyDebeTenerPropiedadesCorrectas() {
            // When
            AdvisorStatus status = AdvisorStatus.BUSY;

            // Then
            assertThat(status.getDescription()).isEqualTo("Atendiendo cliente");
            assertThat(status.canReceiveAssignments()).isFalse();
        }

        @Test
        @DisplayName("OFFLINE debe tener propiedades correctas")
        void offlineDebeTenerPropiedadesCorrectas() {
            // When
            AdvisorStatus status = AdvisorStatus.OFFLINE;

            // Then
            assertThat(status.getDescription()).isEqualTo("No disponible");
            assertThat(status.canReceiveAssignments()).isFalse();
        }
    }

    @Nested
    @DisplayName("Lógica de Negocio")
    class LogicaDeNegocio {

        @Test
        @DisplayName("Solo AVAILABLE puede recibir asignaciones")
        void soloAvailablePuedeRecibirAsignaciones() {
            // When & Then
            assertThat(AdvisorStatus.AVAILABLE.canReceiveAssignments()).isTrue();
            assertThat(AdvisorStatus.BUSY.canReceiveAssignments()).isFalse();
            assertThat(AdvisorStatus.OFFLINE.canReceiveAssignments()).isFalse();
        }
    }
}