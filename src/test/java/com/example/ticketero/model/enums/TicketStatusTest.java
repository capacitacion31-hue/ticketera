package com.example.ticketero.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TicketStatus Enum Tests")
class TicketStatusTest {

    @Nested
    @DisplayName("Valores del Enum")
    class ValoresDelEnum {

        @Test
        @DisplayName("Debe tener todos los valores esperados")
        void debeTenerTodosLosValoresEsperados() {
            // When
            TicketStatus[] values = TicketStatus.values();

            // Then
            assertThat(values).hasSize(6);
            assertThat(values).containsExactly(
                TicketStatus.EN_ESPERA,
                TicketStatus.PROXIMO,
                TicketStatus.ATENDIENDO,
                TicketStatus.COMPLETADO,
                TicketStatus.CANCELADO,
                TicketStatus.NO_ATENDIDO
            );
        }
    }

    @Nested
    @DisplayName("Estados Activos")
    class EstadosActivos {

        @Test
        @DisplayName("EN_ESPERA debe ser activo")
        void enEsperaDebeSerActivo() {
            // When
            TicketStatus status = TicketStatus.EN_ESPERA;

            // Then
            assertThat(status.getDescription()).isEqualTo("Esperando asignación");
            assertThat(status.isActive()).isTrue();
        }

        @Test
        @DisplayName("PROXIMO debe ser activo")
        void proximoDebeSerActivo() {
            // When
            TicketStatus status = TicketStatus.PROXIMO;

            // Then
            assertThat(status.getDescription()).isEqualTo("Próximo a ser atendido");
            assertThat(status.isActive()).isTrue();
        }

        @Test
        @DisplayName("ATENDIENDO debe ser activo")
        void atendiendoDebeSerActivo() {
            // When
            TicketStatus status = TicketStatus.ATENDIENDO;

            // Then
            assertThat(status.getDescription()).isEqualTo("Siendo atendido");
            assertThat(status.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Estados Inactivos")
    class EstadosInactivos {

        @Test
        @DisplayName("COMPLETADO debe ser inactivo")
        void completadoDebeSerInactivo() {
            // When
            TicketStatus status = TicketStatus.COMPLETADO;

            // Then
            assertThat(status.getDescription()).isEqualTo("Atención finalizada");
            assertThat(status.isActive()).isFalse();
        }

        @Test
        @DisplayName("CANCELADO debe ser inactivo")
        void canceladoDebeSerInactivo() {
            // When
            TicketStatus status = TicketStatus.CANCELADO;

            // Then
            assertThat(status.getDescription()).isEqualTo("Cancelado");
            assertThat(status.isActive()).isFalse();
        }

        @Test
        @DisplayName("NO_ATENDIDO debe ser inactivo")
        void noAtendidoDebeSerInactivo() {
            // When
            TicketStatus status = TicketStatus.NO_ATENDIDO;

            // Then
            assertThat(status.getDescription()).isEqualTo("Cliente no se presentó");
            assertThat(status.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Lógica de Negocio")
    class LogicaDeNegocio {

        @Test
        @DisplayName("Debe identificar correctamente estados activos")
        void debeIdentificarCorrectamenteEstadosActivos() {
            // When & Then
            assertThat(TicketStatus.EN_ESPERA.isActive()).isTrue();
            assertThat(TicketStatus.PROXIMO.isActive()).isTrue();
            assertThat(TicketStatus.ATENDIENDO.isActive()).isTrue();
            assertThat(TicketStatus.COMPLETADO.isActive()).isFalse();
            assertThat(TicketStatus.CANCELADO.isActive()).isFalse();
            assertThat(TicketStatus.NO_ATENDIDO.isActive()).isFalse();
        }
    }
}