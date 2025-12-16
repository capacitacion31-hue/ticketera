package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.DashboardResponse;
import com.example.ticketero.model.dto.response.PerformanceSummaryResponse;
import com.example.ticketero.model.dto.response.QueueSummaryResponse;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.QueueStatsRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final QueueStatsRepository queueStatsRepository;
    private final QueueService queueService;

    public DashboardResponse getDashboard() {
        // Calcular summary general
        DashboardResponse.DashboardSummary summary = calculateSummary();
        
        // Obtener resumen de colas
        List<QueueSummaryResponse> queuesSummary = queueService.getAllQueuesSummary();
        
        // Generar alertas
        List<DashboardResponse.AlertResponse> alerts = generateAlerts(queuesSummary);

        return new DashboardResponse(
            LocalDateTime.now(),
            summary,
            queuesSummary,
            alerts
        );
    }

    public PerformanceSummaryResponse getPerformanceSummary() {
        // Calcular métricas de performance
        long totalCompleted = Arrays.stream(QueueType.values())
            .mapToLong(qt -> queueStatsRepository.countTodayByQueueAndStatus(qt, TicketStatus.COMPLETADO))
            .sum();

        Double avgServiceTime = Arrays.stream(QueueType.values())
            .mapToDouble(qt -> {
                Double avg = queueStatsRepository.getAverageServiceTimeToday(qt);
                return avg != null ? avg : qt.getAverageTimeMinutes();
            })
            .average()
            .orElse(15.0);

        Double avgWaitTime = Arrays.stream(QueueType.values())
            .mapToDouble(qt -> {
                Double avg = queueStatsRepository.getAverageWaitTimeToday(qt);
                return avg != null ? avg : 0.0;
            })
            .average()
            .orElse(0.0);

        PerformanceSummaryResponse.PerformanceMetrics performance = 
            new PerformanceSummaryResponse.PerformanceMetrics(
                avgServiceTime.intValue(),
                avgServiceTime,
                avgWaitTime.intValue(),
                87.5, // Efficiency placeholder
                4.2,  // Customer satisfaction placeholder
                "10:00-11:00", // Peak hours placeholder
                (int) totalCompleted,
                93.3  // Service time accuracy placeholder
            );

        Map<String, String> trends = Map.of(
            "serviceTimeVsYesterday", "-5%",
            "waitTimeVsYesterday", "+8%",
            "efficiencyVsYesterday", "+2%"
        );

        List<String> recommendations = List.of(
            "Considerar asignar más asesores en horario 10-11",
            "Revisar proceso de cola CAJA por alta carga"
        );

        return new PerformanceSummaryResponse(
            LocalDate.now(),
            performance,
            trends,
            recommendations
        );
    }

    private DashboardResponse.DashboardSummary calculateSummary() {
        long totalToday = Arrays.stream(QueueType.values())
            .mapToLong(qt -> queueStatsRepository.countTodayByQueueAndStatus(qt, TicketStatus.COMPLETADO))
            .sum();

        long waiting = ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA) +
                       ticketRepository.countByQueueTypeAndStatus(QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA) +
                       ticketRepository.countByQueueTypeAndStatus(QueueType.EMPRESAS, TicketStatus.EN_ESPERA) +
                       ticketRepository.countByQueueTypeAndStatus(QueueType.GERENCIA, TicketStatus.EN_ESPERA);
        
        long serving = ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.ATENDIENDO) +
                      ticketRepository.countByQueueTypeAndStatus(QueueType.PERSONAL_BANKER, TicketStatus.ATENDIENDO) +
                      ticketRepository.countByQueueTypeAndStatus(QueueType.EMPRESAS, TicketStatus.ATENDIENDO) +
                      ticketRepository.countByQueueTypeAndStatus(QueueType.GERENCIA, TicketStatus.ATENDIENDO);
        long completed = totalToday;

        // Contar tickets críticos
        LocalDateTime now = LocalDateTime.now();
        List<com.example.ticketero.model.entity.Ticket> criticalTickets = Arrays.stream(QueueType.values())
            .flatMap(qt -> {
                LocalDateTime timeLimit = now.minusMinutes(qt.getMaxWaitTimeMinutes());
                return ticketRepository.findCriticalTickets(timeLimit).stream();
            })
            .toList();

        long activeAdvisors = advisorRepository.countByStatus(AdvisorStatus.AVAILABLE) + 
                            advisorRepository.countByStatus(AdvisorStatus.BUSY);

        return new DashboardResponse.DashboardSummary(
            (int) totalToday,
            (int) waiting,
            (int) serving,
            (int) completed,
            criticalTickets.size(),
            (int) activeAdvisors,
            22, // Average wait time placeholder
            "OPERATIONAL"
        );
    }

    private List<DashboardResponse.AlertResponse> generateAlerts(List<QueueSummaryResponse> queues) {
        List<DashboardResponse.AlertResponse> alerts = queues.stream()
            .filter(q -> q.criticalTickets() > 0)
            .map(q -> new DashboardResponse.AlertResponse(
                "TICKETS_CRITICOS",
                "HIGH",
                q.criticalTickets() + " tickets han excedido tiempo límite en cola " + q.queueType().getDisplayName(),
                q.criticalTickets(),
                "Asignar asesores adicionales a cola " + q.queueType().getDisplayName(),
                LocalDateTime.now()
            ))
            .toList();

        // Agregar alerta de cola sobrecargada
        queues.stream()
            .filter(q -> "HIGH_LOAD".equals(q.status()))
            .forEach(q -> alerts.add(new DashboardResponse.AlertResponse(
                "COLA_SOBRECARGADA",
                "MEDIUM",
                "Cola " + q.queueType().getDisplayName() + " con alta carga (" + q.ticketsWaiting() + " tickets esperando)",
                q.ticketsWaiting(),
                "Reasignar asesores a cola " + q.queueType().getDisplayName(),
                LocalDateTime.now()
            )));

        return alerts;
    }
}