package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.QueueStatsResponse;
import com.example.ticketero.model.dto.response.QueueSummaryResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.QueueStatsRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueService - Unit Tests")
class QueueServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private QueueStatsRepository queueStatsRepository;

    @InjectMocks
    private QueueService queueService;

    @Nested
    @DisplayName("getAllQueuesSummary()")
    class ObtenerResumenTodasColas {

        @Test
        @DisplayName("debe retornar resumen de todas las colas")
        void getAllQueuesSummary_debeRetornarTodasLasColas() {
            // Given
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(5L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(20L);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(10.0);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            List<QueueSummaryResponse> summaries = queueService.getAllQueuesSummary();

            // Then
            assertThat(summaries).hasSize(4); // CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA
            assertThat(summaries).extracting(QueueSummaryResponse::queueType)
                .containsExactlyInAnyOrder(QueueType.CAJA, QueueType.PERSONAL_BANKER, QueueType.EMPRESAS, QueueType.GERENCIA);
        }
    }

    @Nested
    @DisplayName("getQueueSummary()")
    class ObtenerResumenCola {

        @Test
        @DisplayName("debe calcular resumen correctamente")
        void getQueueSummary_debeCalcularCorrectamente() {
            // Given
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(8L);
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.ATENDIENDO))
                .thenReturn(2L);
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.CAJA, TicketStatus.COMPLETADO))
                .thenReturn(45L);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.CAJA))
                .thenReturn(12.5);
            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(Collections.emptyList());

            // When
            QueueSummaryResponse summary = queueService.getQueueSummary(QueueType.CAJA);

            // Then
            assertThat(summary.queueType()).isEqualTo(QueueType.CAJA);
            assertThat(summary.displayName()).isEqualTo("Caja");
            assertThat(summary.ticketsWaiting()).isEqualTo(8);
            assertThat(summary.ticketsBeingServed()).isEqualTo(2);
            assertThat(summary.totalTicketsToday()).isEqualTo(45);
            assertThat(summary.averageWaitTimeToday()).isEqualTo(12);
            assertThat(summary.status()).isEqualTo("MEDIUM_LOAD"); // 8 tickets waiting
        }

        @Test
        @DisplayName("con tickets críticos → debe marcar status CRITICAL")
        void getQueueSummary_conTicketsCriticos_debeMarcarCritical() {
            // Given - Necesitamos >2 tickets críticos para CRITICAL
            List<Ticket> criticalTickets = List.of(
                ticketWaiting().queueType(QueueType.CAJA).build(),
                ticketWaiting().queueType(QueueType.CAJA).build(),
                ticketWaiting().queueType(QueueType.CAJA).build(), // 3 tickets de CAJA
                ticketWaiting().queueType(QueueType.PERSONAL_BANKER).build()
            );

            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(5L); // Menos de 10 para que no sea HIGH_LOAD
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.ATENDIENDO))
                .thenReturn(1L);
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.CAJA, TicketStatus.COMPLETADO))
                .thenReturn(10L);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.CAJA))
                .thenReturn(5.0);
            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(criticalTickets);

            // When
            QueueSummaryResponse summary = queueService.getQueueSummary(QueueType.CAJA);

            // Then
            assertThat(summary.criticalTickets()).isEqualTo(3); // 3 tickets de CAJA
            assertThat(summary.status()).isEqualTo("CRITICAL"); // >2 críticos
        }

        @Test
        @DisplayName("sin datos de tiempo promedio → debe usar 0")
        void getQueueSummary_sinDatosTiempo_debeUsar0() {
            // Given
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(1L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(5L);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(null);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            QueueSummaryResponse summary = queueService.getQueueSummary(QueueType.CAJA);

            // Then
            assertThat(summary.averageWaitTimeToday()).isEqualTo(0);
        }

        @Test
        @DisplayName("debe determinar status según carga")
        void getQueueSummary_debeDeterminarStatusSegunCarga() {
            // Given - NORMAL (≤5 waiting, 0 critical)
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(3L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(5.0);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            QueueSummaryResponse summary = queueService.getQueueSummary(QueueType.CAJA);

            // Then
            assertThat(summary.status()).isEqualTo("NORMAL");
        }
    }

    @Nested
    @DisplayName("getQueueStats()")
    class ObtenerEstadisticasCola {

        @Test
        @DisplayName("debe calcular estadísticas completas")
        void getQueueStats_debeCalcularEstadisticasCompletas() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.CAJA, TicketStatus.COMPLETADO))
                .thenReturn(50L);
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(8L);
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.ATENDIENDO))
                .thenReturn(3L);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.CAJA))
                .thenReturn(4.5);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.CAJA))
                .thenReturn(15.0);
            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(Collections.emptyList());

            // When
            QueueStatsResponse stats = queueService.getQueueStats(QueueType.CAJA);

            // Then
            assertThat(stats.queueType()).isEqualTo(QueueType.CAJA);
            assertThat(stats.ticketsCompleted()).isEqualTo(50);
            assertThat(stats.ticketsWaiting()).isEqualTo(8);
            assertThat(stats.ticketsBeingServed()).isEqualTo(3);
            assertThat(stats.averageServiceTimeMinutes()).isEqualTo(4);
            assertThat(stats.averageWaitTimeMinutes()).isEqualTo(15);
            assertThat(stats.efficiency()).isGreaterThan(0.0);
            assertThat(stats.trends()).containsKeys("waitTimeVsPrevious", "serviceTimeVsPrevious");
        }

        @Test
        @DisplayName("sin datos de tiempo → debe usar valores por defecto")
        void getQueueStats_sinDatosTiempo_debeUsarDefaults() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(5L);
            when(queueStatsRepository.getAverageServiceTimeToday(any())).thenReturn(null);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(null);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            QueueStatsResponse stats = queueService.getQueueStats(QueueType.CAJA);

            // Then
            assertThat(stats.averageServiceTimeMinutes()).isEqualTo(5); // Default de CAJA
            assertThat(stats.averageWaitTimeMinutes()).isEqualTo(0);
            assertThat(stats.efficiency()).isEqualTo(85.0); // Default
        }

        @Test
        @DisplayName("debe calcular eficiencia correctamente")
        void getQueueStats_debeCalcularEficiencia() {
            // Given - tiempo real = tiempo estimado → 100% eficiencia
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(5L);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.CAJA))
                .thenReturn(5.0); // Igual al estimado de CAJA
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(10.0);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            QueueStatsResponse stats = queueService.getQueueStats(QueueType.CAJA);

            // Then
            assertThat(stats.efficiency()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Métodos privados")
    class MetodosPrivados {

        @Test
        @DisplayName("determineQueueStatus debe clasificar correctamente")
        void determineQueueStatus_debeClasificarCorrectamente() {
            // Given - Testear indirectamente a través de getQueueSummary
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(5.0);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // HIGH_LOAD (>10 waiting)
            when(ticketRepository.countByQueueTypeAndStatus(any(), eq(TicketStatus.EN_ESPERA)))
                .thenReturn(15L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), eq(TicketStatus.ATENDIENDO)))
                .thenReturn(2L);

            // When
            QueueSummaryResponse summary = queueService.getQueueSummary(QueueType.CAJA);

            // Then
            assertThat(summary.status()).isEqualTo("HIGH_LOAD");
        }

        @Test
        @DisplayName("calculateEfficiency debe manejar casos edge")
        void calculateEfficiency_debeManejarcasosEdge() {
            // Given - Testear indirectamente con tiempo muy diferente
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(5L);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.CAJA))
                .thenReturn(10.0); // Doble del estimado (5 min)
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(10.0);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(Collections.emptyList());

            // When
            QueueStatsResponse stats = queueService.getQueueStats(QueueType.CAJA);

            // Then
            // Eficiencia = 100 - |10-5|/5 * 100 = 100 - 100 = 0
            assertThat(stats.efficiency()).isEqualTo(0.0);
        }
    }
}