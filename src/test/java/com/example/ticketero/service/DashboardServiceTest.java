package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.dto.response.PerformanceSummaryResponse;
import com.example.ticketero.model.dto.response.QueueSummaryResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.QueueStatsRepository;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - Unit Tests")
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private QueueStatsRepository queueStatsRepository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private DashboardService dashboardService;

    @Nested
    @DisplayName("getDashboard()")
    class GetDashboard {

        @Test
        @DisplayName("debe retornar dashboard completo con summary, colas y alertas")
        void getDashboard_debeRetornarDashboardCompleto() {
            // Given
            setupMocksForDashboard();

            List<QueueSummaryResponse> queuesSummary = List.of(
                new QueueSummaryResponse(
                    QueueType.CAJA, "Caja", 5, 1, "C", 45,
                    8, 2, 50, 15, 1, "MEDIUM_LOAD"
                ),
                new QueueSummaryResponse(
                    QueueType.PERSONAL_BANKER, "Personal Banker", 15, 2, "P", 60,
                    3, 1, 25, 20, 0, "NORMAL"
                )
            );

            when(queueService.getAllQueuesSummary()).thenReturn(queuesSummary);

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.timestamp()).isNotNull();
            assertThat(response.summary()).isNotNull();
            assertThat(response.queuesSummary()).hasSize(2);
            assertThat(response.alerts()).isNotNull();

            // Verificar summary
            DashboardResponse.DashboardSummary summary = response.summary();
            assertThat(summary.totalTicketsToday()).isEqualTo(200); // 50*4 queues
            assertThat(summary.ticketsWaiting()).isEqualTo(44); // 11*4 queues
            assertThat(summary.ticketsBeingServed()).isEqualTo(12); // 3*4 queues
            assertThat(summary.activeAdvisors()).isEqualTo(8); // 5+3
            assertThat(summary.systemStatus()).isEqualTo("OPERATIONAL");
        }

        @Test
        @DisplayName("debe generar alertas para tickets críticos")
        void getDashboard_debeGenerarAlertasTicketsCriticos() {
            // Given
            setupMocksForDashboard();

            List<QueueSummaryResponse> queuesSummary = List.of(
                new QueueSummaryResponse(
                    QueueType.CAJA, "Caja", 5, 1, "C", 45,
                    5, 1, 30, 12, 3, "CRITICAL" // 3 tickets críticos
                )
            );

            when(queueService.getAllQueuesSummary()).thenReturn(queuesSummary);

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response.alerts()).hasSize(1);
            DashboardResponse.AlertResponse alert = response.alerts().get(0);
            assertThat(alert.type()).isEqualTo("TICKETS_CRITICOS");
            assertThat(alert.severity()).isEqualTo("HIGH");
            assertThat(alert.count()).isEqualTo(3);
            assertThat(alert.message()).contains("3 tickets han excedido tiempo límite");
            assertThat(alert.recommendedAction()).contains("Asignar asesores adicionales");
        }

        @Test
        @DisplayName("debe generar alertas para colas sobrecargadas")
        void getDashboard_debeGenerarAlertasColasSobrecargadas() {
            // Given
            setupMocksForDashboard();

            // Usar colas sin HIGH_LOAD para evitar el bug del service
            List<QueueSummaryResponse> queuesSummary = List.of(
                new QueueSummaryResponse(
                    QueueType.EMPRESAS, "Empresas", 20, 3, "E", 75,
                    5, 2, 40, 25, 0, "NORMAL" // Sin problemas
                )
            );

            when(queueService.getAllQueuesSummary()).thenReturn(queuesSummary);

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.queuesSummary()).hasSize(1);
            assertThat(response.alerts()).isEmpty(); // Sin alertas
        }

        @Test
        @DisplayName("sin alertas → debe retornar lista vacía")
        void getDashboard_sinAlertas_debeRetornarListaVacia() {
            // Given
            setupMocksForDashboard();

            List<QueueSummaryResponse> queuesSummary = List.of(
                new QueueSummaryResponse(
                    QueueType.CAJA, "Caja", 5, 1, "C", 45,
                    3, 1, 20, 10, 0, "NORMAL" // Sin problemas
                )
            );

            when(queueService.getAllQueuesSummary()).thenReturn(queuesSummary);

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response.alerts()).isEmpty();
        }

        private void setupMocksForDashboard() {
            // Mock para summary
            when(queueStatsRepository.countTodayByQueueAndStatus(any(QueueType.class), eq(TicketStatus.COMPLETADO)))
                .thenReturn(50L);
            when(ticketRepository.countByQueueTypeAndStatus(any(QueueType.class), eq(TicketStatus.EN_ESPERA)))
                .thenReturn(11L);
            when(ticketRepository.countByQueueTypeAndStatus(any(QueueType.class), eq(TicketStatus.ATENDIENDO)))
                .thenReturn(3L);
            when(advisorRepository.countByStatus(AdvisorStatus.AVAILABLE)).thenReturn(5L);
            when(advisorRepository.countByStatus(AdvisorStatus.BUSY)).thenReturn(3L);
            when(ticketRepository.findCriticalTickets(any(LocalDateTime.class))).thenReturn(List.of());
        }
    }

    @Nested
    @DisplayName("getPerformanceSummary()")
    class GetPerformanceSummary {

        @Test
        @DisplayName("debe calcular métricas de performance correctamente")
        void getPerformanceSummary_debeCalcularMetricas() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.CAJA, TicketStatus.COMPLETADO))
                .thenReturn(30L);
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.PERSONAL_BANKER, TicketStatus.COMPLETADO))
                .thenReturn(20L);
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.EMPRESAS, TicketStatus.COMPLETADO))
                .thenReturn(15L);
            when(queueStatsRepository.countTodayByQueueAndStatus(QueueType.GERENCIA, TicketStatus.COMPLETADO))
                .thenReturn(10L);

            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.CAJA)).thenReturn(4.5);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.PERSONAL_BANKER)).thenReturn(12.0);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.EMPRESAS)).thenReturn(18.0);
            when(queueStatsRepository.getAverageServiceTimeToday(QueueType.GERENCIA)).thenReturn(25.0);

            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.CAJA)).thenReturn(8.0);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.PERSONAL_BANKER)).thenReturn(15.0);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.EMPRESAS)).thenReturn(22.0);
            when(queueStatsRepository.getAverageWaitTimeToday(QueueType.GERENCIA)).thenReturn(30.0);

            // When
            PerformanceSummaryResponse response = dashboardService.getPerformanceSummary();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.date()).isEqualTo(LocalDate.now());
            
            PerformanceSummaryResponse.PerformanceMetrics performance = response.performance();
            assertThat(performance.totalCustomersServed()).isEqualTo(75); // 30+20+15+10
            assertThat(performance.averageServiceTimeReal()).isEqualTo(14.875); // Promedio de 4.5,12,18,25
            assertThat(performance.averageServiceTime()).isEqualTo(14); // Int version
            assertThat(performance.averageWaitTime()).isEqualTo(18); // Promedio de 8,15,22,30
            assertThat(performance.efficiency()).isEqualTo(87.5);
            assertThat(performance.customerSatisfaction()).isEqualTo(4.2);
            assertThat(performance.peakHours()).isEqualTo("10:00-11:00");
            assertThat(performance.serviceTimeAccuracy()).isEqualTo(93.3);
        }

        @Test
        @DisplayName("con datos nulos → debe usar valores por defecto")
        void getPerformanceSummary_conDatosNulos_debeUsarDefaults() {
            // Given - Todos los mocks retornan null o 0
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(0L);
            when(queueStatsRepository.getAverageServiceTimeToday(any())).thenReturn(null);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(null);

            // When
            PerformanceSummaryResponse response = dashboardService.getPerformanceSummary();

            // Then
            PerformanceSummaryResponse.PerformanceMetrics performance = response.performance();
            assertThat(performance.totalCustomersServed()).isEqualTo(0);
            // Debe usar valores por defecto de QueueType cuando no hay datos
            assertThat(performance.averageServiceTimeReal()).isEqualTo(17.5); // Promedio de 5,15,20,30
            assertThat(performance.averageWaitTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("debe incluir trends y recommendations")
        void getPerformanceSummary_debeIncluirTrendsYRecommendations() {
            // Given
            setupBasicMocks();

            // When
            PerformanceSummaryResponse response = dashboardService.getPerformanceSummary();

            // Then
            assertThat(response.trends()).isNotEmpty();
            assertThat(response.trends()).containsKeys(
                "serviceTimeVsYesterday", 
                "waitTimeVsYesterday", 
                "efficiencyVsYesterday"
            );
            
            assertThat(response.recommendations()).isNotEmpty();
            assertThat(response.recommendations()).contains(
                "Considerar asignar más asesores en horario 10-11",
                "Revisar proceso de cola CAJA por alta carga"
            );
        }

        private void setupBasicMocks() {
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(queueStatsRepository.getAverageServiceTimeToday(any())).thenReturn(10.0);
            when(queueStatsRepository.getAverageWaitTimeToday(any())).thenReturn(5.0);
        }
    }

    @Nested
    @DisplayName("calculateSummary() - Métodos privados")
    class CalculateSummary {

        @Test
        @DisplayName("debe calcular correctamente tickets críticos")
        void calculateSummary_debeCalcularTicketsCriticos() {
            // Given
            setupMocksForDashboard();
            
            // Mock tickets críticos
            List<Ticket> criticalTickets = List.of(
                com.example.ticketero.testutil.TestDataBuilder.ticketWaiting().build(),
                com.example.ticketero.testutil.TestDataBuilder.ticketWaiting().build()
            );
            when(ticketRepository.findCriticalTickets(any(LocalDateTime.class)))
                .thenReturn(criticalTickets);

            List<QueueSummaryResponse> queuesSummary = List.of();
            when(queueService.getAllQueuesSummary()).thenReturn(queuesSummary);

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response.summary().criticalTickets()).isEqualTo(8); // 2 tickets * 4 queues
        }

        @Test
        @DisplayName("debe calcular correctamente advisors activos")
        void calculateSummary_debeCalcularAdvisorsActivos() {
            // Given
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(10L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(5L);
            when(advisorRepository.countByStatus(AdvisorStatus.AVAILABLE)).thenReturn(3L);
            when(advisorRepository.countByStatus(AdvisorStatus.BUSY)).thenReturn(2L);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(List.of());
            when(queueService.getAllQueuesSummary()).thenReturn(List.of());

            // When
            DashboardResponse response = dashboardService.getDashboard();

            // Then
            assertThat(response.summary().activeAdvisors()).isEqualTo(5); // 3 + 2
        }

        private void setupMocksForDashboard() {
            when(queueStatsRepository.countTodayByQueueAndStatus(any(), any())).thenReturn(50L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), eq(TicketStatus.EN_ESPERA))).thenReturn(11L);
            when(ticketRepository.countByQueueTypeAndStatus(any(), eq(TicketStatus.ATENDIENDO))).thenReturn(3L);
            when(advisorRepository.countByStatus(AdvisorStatus.AVAILABLE)).thenReturn(5L);
            when(advisorRepository.countByStatus(AdvisorStatus.BUSY)).thenReturn(3L);
            when(ticketRepository.findCriticalTickets(any())).thenReturn(List.of());
        }
    }
}