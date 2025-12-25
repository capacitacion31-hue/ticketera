package com.example.ticketero.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueType Enum Tests")
class QueueTypeTest {

    @Nested
    @DisplayName("Valores del Enum")
    class ValoresDelEnum {

        @Test
        @DisplayName("Debe tener todos los valores esperados")
        void debeTenerTodosLosValoresEsperados() {
            // When
            QueueType[] values = QueueType.values();

            // Then
            assertThat(values).hasSize(4);
            assertThat(values).containsExactly(
                QueueType.CAJA,
                QueueType.PERSONAL_BANKER,
                QueueType.EMPRESAS,
                QueueType.GERENCIA
            );
        }
    }

    @Nested
    @DisplayName("Propiedades CAJA")
    class PropiedadesCaja {

        @Test
        @DisplayName("CAJA debe tener propiedades correctas")
        void cajaDebeTenerPropiedadesCorrectas() {
            // When
            QueueType queue = QueueType.CAJA;

            // Then
            assertThat(queue.getDisplayName()).isEqualTo("Caja");
            assertThat(queue.getAverageTimeMinutes()).isEqualTo(5);
            assertThat(queue.getPriority()).isEqualTo(1);
            assertThat(queue.getPrefix()).isEqualTo("C");
            assertThat(queue.getMaxWaitTimeMinutes()).isEqualTo(45);
        }
    }

    @Nested
    @DisplayName("Propiedades PERSONAL_BANKER")
    class PropiedadesPersonalBanker {

        @Test
        @DisplayName("PERSONAL_BANKER debe tener propiedades correctas")
        void personalBankerDebeTenerPropiedadesCorrectas() {
            // When
            QueueType queue = QueueType.PERSONAL_BANKER;

            // Then
            assertThat(queue.getDisplayName()).isEqualTo("Personal Banker");
            assertThat(queue.getAverageTimeMinutes()).isEqualTo(15);
            assertThat(queue.getPriority()).isEqualTo(2);
            assertThat(queue.getPrefix()).isEqualTo("P");
            assertThat(queue.getMaxWaitTimeMinutes()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("Propiedades EMPRESAS")
    class PropiedadesEmpresas {

        @Test
        @DisplayName("EMPRESAS debe tener propiedades correctas")
        void empresasDebeTenerPropiedadesCorrectas() {
            // When
            QueueType queue = QueueType.EMPRESAS;

            // Then
            assertThat(queue.getDisplayName()).isEqualTo("Empresas");
            assertThat(queue.getAverageTimeMinutes()).isEqualTo(20);
            assertThat(queue.getPriority()).isEqualTo(3);
            assertThat(queue.getPrefix()).isEqualTo("E");
            assertThat(queue.getMaxWaitTimeMinutes()).isEqualTo(75);
        }
    }

    @Nested
    @DisplayName("Propiedades GERENCIA")
    class PropiedadesGerencia {

        @Test
        @DisplayName("GERENCIA debe tener propiedades correctas")
        void gerenciaDebeTenerPropiedadesCorrectas() {
            // When
            QueueType queue = QueueType.GERENCIA;

            // Then
            assertThat(queue.getDisplayName()).isEqualTo("Gerencia");
            assertThat(queue.getAverageTimeMinutes()).isEqualTo(30);
            assertThat(queue.getPriority()).isEqualTo(4);
            assertThat(queue.getPrefix()).isEqualTo("G");
            assertThat(queue.getMaxWaitTimeMinutes()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("Ordenamiento por Prioridad")
    class OrdenamientoPorPrioridad {

        @Test
        @DisplayName("Debe tener prioridades en orden ascendente")
        void debeTenerPrioridadesEnOrdenAscendente() {
            // When & Then
            assertThat(QueueType.CAJA.getPriority()).isLessThan(QueueType.PERSONAL_BANKER.getPriority());
            assertThat(QueueType.PERSONAL_BANKER.getPriority()).isLessThan(QueueType.EMPRESAS.getPriority());
            assertThat(QueueType.EMPRESAS.getPriority()).isLessThan(QueueType.GERENCIA.getPriority());
        }
    }
}