package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Advisor Entity Tests")
class AdvisorTest {

    @Nested
    @DisplayName("Constructor y Builder")
    class ConstructorYBuilder {

        @Test
        @DisplayName("Debe crear advisor con builder correctamente")
        void debeCrearAdvisorConBuilder() {
            // When
            Advisor advisor = Advisor.builder()
                .name("Juan Pérez")
                .email("juan.perez@banco.com")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(1)
                .queueTypes(List.of(QueueType.CAJA))
                .build();

            // Then
            assertThat(advisor.getName()).isEqualTo("Juan Pérez");
            assertThat(advisor.getEmail()).isEqualTo("juan.perez@banco.com");
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(advisor.getModuleNumber()).isEqualTo(1);
            assertThat(advisor.getQueueTypes()).containsExactly(QueueType.CAJA);
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getWorkloadMinutes()).isEqualTo(0);
            assertThat(advisor.getAverageServiceTimeMinutes()).isEqualTo(BigDecimal.ZERO);
            assertThat(advisor.getTotalTicketsServedToday()).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe crear advisor con constructor sin argumentos")
        void debeCrearAdvisorConConstructorSinArgumentos() {
            // When
            Advisor advisor = new Advisor();

            // Then
            assertThat(advisor).isNotNull();
            assertThat(advisor.getId()).isNull();
            assertThat(advisor.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("Callbacks JPA")
    class CallbacksJPA {

        @Test
        @DisplayName("Debe establecer timestamps en onCreate")
        void debeEstablecerTimestampsEnOnCreate() {
            // Given
            Advisor advisor = new Advisor();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            advisor.onCreate();

            // Then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertThat(advisor.getCreatedAt()).isBetween(before, after);
            assertThat(advisor.getUpdatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("Debe actualizar updatedAt en onUpdate")
        void debeActualizarUpdatedAtEnOnUpdate() {
            // Given
            Advisor advisor = new Advisor();
            advisor.onCreate();
            LocalDateTime originalUpdatedAt = advisor.getUpdatedAt();
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            advisor.onUpdate();

            // Then
            assertThat(advisor.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Valores por Defecto")
    class ValoresPorDefecto {

        @Test
        @DisplayName("Debe tener valores por defecto correctos")
        void debeTenerValoresPorDefectoCorrectos() {
            // When
            Advisor advisor = Advisor.builder()
                .name("Test")
                .email("test@banco.com")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(1)
                .queueTypes(List.of(QueueType.CAJA))
                .build();

            // Then
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getWorkloadMinutes()).isEqualTo(0);
            assertThat(advisor.getAverageServiceTimeMinutes()).isEqualTo(BigDecimal.ZERO);
            assertThat(advisor.getTotalTicketsServedToday()).isEqualTo(0);
        }
    }
}