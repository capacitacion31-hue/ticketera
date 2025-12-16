package com.example.ticketero.scheduler;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.QueueStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsScheduler {

    private final QueueStatsRepository queueStatsRepository;
    private final AdvisorRepository advisorRepository;

    @Scheduled(fixedDelay = 60000) // Cada minuto
    public void logSystemMetrics() {
        try {
            logQueueMetrics();
            logAdvisorMetrics();
        } catch (Exception e) {
            log.error("Error logging system metrics", e);
        }
    }

    @Scheduled(cron = "0 */15 * * * ?") // Cada 15 minutos
    public void logDetailedMetrics() {
        try {
            for (QueueType queueType : QueueType.values()) {
                long waiting = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.EN_ESPERA);
                long serving = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.ATENDIENDO);
                long completed = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.COMPLETADO);
                
                Double avgServiceTime = queueStatsRepository.getAverageServiceTimeToday(queueType);
                Double avgWaitTime = queueStatsRepository.getAverageWaitTimeToday(queueType);
                
                log.info("QUEUE_METRICS [{}] - Waiting: {}, Serving: {}, Completed: {}, AvgService: {}min, AvgWait: {}min",
                    queueType, waiting, serving, completed, 
                    avgServiceTime != null ? avgServiceTime.intValue() : "N/A",
                    avgWaitTime != null ? avgWaitTime.intValue() : "N/A");
            }
        } catch (Exception e) {
            log.error("Error logging detailed metrics", e);
        }
    }

    private void logQueueMetrics() {
        long totalWaiting = 0;
        long totalServing = 0;
        
        for (QueueType queueType : QueueType.values()) {
            long waiting = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.EN_ESPERA);
            long serving = queueStatsRepository.countTodayByQueueAndStatus(queueType, TicketStatus.ATENDIENDO);
            
            totalWaiting += waiting;
            totalServing += serving;
        }
        
        log.info("SYSTEM_METRICS - Total waiting: {}, Total serving: {}", totalWaiting, totalServing);
    }

    private void logAdvisorMetrics() {
        long available = advisorRepository.countByStatus(AdvisorStatus.AVAILABLE);
        long busy = advisorRepository.countByStatus(AdvisorStatus.BUSY);
        long offline = advisorRepository.countByStatus(AdvisorStatus.OFFLINE);
        
        log.info("ADVISOR_METRICS - Available: {}, Busy: {}, Offline: {}", available, busy, offline);
    }
}