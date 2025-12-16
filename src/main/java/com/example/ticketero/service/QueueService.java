package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.QueueStatsResponse;
import com.example.ticketero.model.dto.response.QueueSummaryResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
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
public class QueueService {

    private final TicketRepository ticketRepository;
    private final QueueStatsRepository queueStatsRepository;

    public List<QueueSummaryResponse> getAllQueuesSummary() {
        return Arrays.stream(QueueType.values())
            .map(this::getQueueSummary)
            .toList();
    }

    public QueueSummaryResponse getQueueSummary(QueueType queueType) {
        long waiting = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.EN_ESPERA);
        long serving = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.ATENDIENDO);
        long completedToday = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.COMPLETADO);
        
        Double avgWaitTime = queueStatsRepository.getAverageWaitTimeToday(queueType);
        int avgWaitTimeToday = avgWaitTime != null ? avgWaitTime.intValue() : 0;
        
        // Detectar tickets críticos
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(queueType.getMaxWaitTimeMinutes());
        List<com.example.ticketero.model.entity.Ticket> criticalTickets = ticketRepository.findCriticalTickets(timeLimit);
        long criticalCount = criticalTickets.stream()
            .filter(t -> t.getQueueType() == queueType)
            .count();

        String status = determineQueueStatus(waiting, criticalCount);

        return new QueueSummaryResponse(
            queueType,
            queueType.getDisplayName(),
            queueType.getAverageTimeMinutes(),
            queueType.getPriority(),
            queueType.getPrefix(),
            queueType.getMaxWaitTimeMinutes(),
            (int) waiting,
            (int) serving,
            (int) completedToday,
            avgWaitTimeToday,
            (int) criticalCount,
            status
        );
    }

    public QueueStatsResponse getQueueStats(QueueType queueType) {
        long completed = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.COMPLETADO);
        long waiting = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.EN_ESPERA);
        long serving = ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.ATENDIENDO);
        
        Double avgServiceTime = queueStatsRepository.getAverageServiceTimeToday(queueType);
        Double avgWaitTime = queueStatsRepository.getAverageWaitTimeToday(queueType);
        
        // Detectar tickets críticos
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(queueType.getMaxWaitTimeMinutes());
        List<com.example.ticketero.model.entity.Ticket> criticalTickets = ticketRepository.findCriticalTickets(timeLimit);
        long criticalCount = criticalTickets.stream()
            .filter(t -> t.getQueueType() == queueType)
            .count();

        // Calcular eficiencia
        double efficiency = calculateEfficiency(avgServiceTime, queueType.getAverageTimeMinutes());

        Map<String, String> trends = Map.of(
            "waitTimeVsPrevious", "+5%",  // Placeholder - implementar cálculo real
            "serviceTimeVsPrevious", "-2%"
        );

        return new QueueStatsResponse(
            queueType,
            LocalDate.now(),
            (int) completed,
            (int) waiting,
            (int) serving,
            avgServiceTime != null ? avgServiceTime.intValue() : queueType.getAverageTimeMinutes(),
            avgWaitTime != null ? avgWaitTime.intValue() : 0,
            (int) criticalCount,
            "10:00-11:00", // Placeholder - implementar cálculo de hora pico
            efficiency,
            trends
        );
    }

    private String determineQueueStatus(long waiting, long critical) {
        if (critical > 2) return "CRITICAL";
        if (waiting > 10) return "HIGH_LOAD";
        if (waiting > 5) return "MEDIUM_LOAD";
        return "NORMAL";
    }

    private double calculateEfficiency(Double actualTime, int estimatedTime) {
        if (actualTime == null) return 85.0; // Default
        return Math.max(0, 100 - Math.abs(actualTime - estimatedTime) / estimatedTime * 100);
    }
}