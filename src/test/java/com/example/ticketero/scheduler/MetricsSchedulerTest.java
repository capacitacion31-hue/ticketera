package com.example.ticketero.scheduler;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.QueueStatsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsScheduler Tests")
class MetricsSchedulerTest {

    @Mock
    private QueueStatsRepository queueStatsRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @InjectMocks
    private MetricsScheduler metricsScheduler;

    @Nested
    @DisplayName("Métricas del Sistema")
    class MetricasDelSistema {

        @Test
        @DisplayName("Debe registrar métricas del sistema exitosamente")
        void logSystemMetrics_debeEjecutarExitosamente() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.EN_ESPERA)))
                .thenReturn(5L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.ATENDIENDO)))
                .thenReturn(2L);
            when(advisorRepository.countByStatus(AdvisorStatus.AVAILABLE)).thenReturn(3L);
            when(advisorRepository.countByStatus(AdvisorStatus.BUSY)).thenReturn(2L);
            when(advisorRepository.countByStatus(AdvisorStatus.OFFLINE)).thenReturn(1L);

            // When
            assertThatCode(() -> metricsScheduler.logSystemMetrics())
                .doesNotThrowAnyException();

            // Then
            verify(queueStatsRepository, atLeastOnce())
                .countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class));
            verify(advisorRepository).countByStatus(AdvisorStatus.AVAILABLE);
            verify(advisorRepository).countByStatus(AdvisorStatus.BUSY);
            verify(advisorRepository).countByStatus(AdvisorStatus.OFFLINE);
        }

        @Test
        @DisplayName("Debe manejar errores al registrar métricas del sistema")
        void logSystemMetrics_debeManejarErrores() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class)))
                .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            assertThatCode(() -> metricsScheduler.logSystemMetrics())
                .doesNotThrowAnyException();

            verify(queueStatsRepository, atLeastOnce())
                .countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class));
        }
    }

    @Nested
    @DisplayName("Métricas Detalladas")
    class MetricasDetalladas {

        @Test
        @DisplayName("Debe registrar métricas detalladas exitosamente")
        void logDetailedMetrics_debeEjecutarExitosamente() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.EN_ESPERA)))
                .thenReturn(3L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.ATENDIENDO)))
                .thenReturn(1L);
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.COMPLETADO)))
                .thenReturn(10L);
            when(queueStatsRepository.getAverageServiceTimeToday(any(QueueType.class)))
                .thenReturn(15.5);
            when(queueStatsRepository.getAverageWaitTimeToday(any(QueueType.class)))
                .thenReturn(8.2);

            // When
            assertThatCode(() -> metricsScheduler.logDetailedMetrics())
                .doesNotThrowAnyException();

            // Then
            verify(queueStatsRepository, times(QueueType.values().length))
                .countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.EN_ESPERA));
            verify(queueStatsRepository, times(QueueType.values().length))
                .countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.ATENDIENDO));
            verify(queueStatsRepository, times(QueueType.values().length))
                .countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.COMPLETADO));
            verify(queueStatsRepository, times(QueueType.values().length))
                .getAverageServiceTimeToday(any(QueueType.class));
            verify(queueStatsRepository, times(QueueType.values().length))
                .getAverageWaitTimeToday(any(QueueType.class));
        }

        @Test
        @DisplayName("Debe manejar valores nulos en métricas detalladas")
        void logDetailedMetrics_debeManejarValoresNulos() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class)))
                .thenReturn(0L);
            when(queueStatsRepository.getAverageServiceTimeToday(any(QueueType.class)))
                .thenReturn(null);
            when(queueStatsRepository.getAverageWaitTimeToday(any(QueueType.class)))
                .thenReturn(null);

            // When
            assertThatCode(() -> metricsScheduler.logDetailedMetrics())
                .doesNotThrowAnyException();

            // Then
            verify(queueStatsRepository, times(QueueType.values().length))
                .getAverageServiceTimeToday(any(QueueType.class));
            verify(queueStatsRepository, times(QueueType.values().length))
                .getAverageWaitTimeToday(any(QueueType.class));
        }

        @Test
        @DisplayName("Debe manejar errores al registrar métricas detalladas")
        void logDetailedMetrics_debeManejarErrores() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class)))
                .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            assertThatCode(() -> metricsScheduler.logDetailedMetrics())
                .doesNotThrowAnyException();

            verify(queueStatsRepository, atLeastOnce())
                .countTodayByQueueAndStatus(any(QueueType.class), any(TicketStatus.class));
        }
    }
}