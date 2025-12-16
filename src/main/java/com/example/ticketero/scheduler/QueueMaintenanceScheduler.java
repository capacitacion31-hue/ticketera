package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueMaintenanceScheduler {

    private final TicketRepository ticketRepository;
    private final MessageService messageService;
    private final AuditService auditService;

    @Scheduled(fixedDelay = 10000) // Cada 10 segundos
    @Transactional
    public void updateQueuePositions() {
        try {
            for (QueueType queueType : QueueType.values()) {
                updatePositionsForQueue(queueType);
            }
        } catch (Exception e) {
            log.error("Error updating queue positions", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Diariamente a medianoche
    @Transactional
    public void dailyCleanup() {
        try {
            cleanupOldTickets();
            resetDailyCounters();
            log.info("Daily cleanup completed");
        } catch (Exception e) {
            log.error("Error in daily cleanup", e);
        }
    }

    @Scheduled(fixedDelay = 60000) // Cada minuto
    @Transactional
    public void checkCriticalTickets() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (QueueType queueType : QueueType.values()) {
                LocalDateTime timeLimit = now.minusMinutes(queueType.getMaxWaitTimeMinutes());
                List<Ticket> criticalTickets = ticketRepository.findCriticalTickets(timeLimit)
                    .stream()
                    .filter(t -> t.getQueueType() == queueType)
                    .filter(t -> t.getStatus() == TicketStatus.EN_ESPERA)
                    .toList();

                for (Ticket ticket : criticalTickets) {
                    log.warn("CRITICAL TICKET DETECTED: {} has been waiting {} minutes (limit: {})",
                        ticket.getNumero(),
                        java.time.Duration.between(ticket.getCreatedAt(), now).toMinutes(),
                        queueType.getMaxWaitTimeMinutes());
                }
            }
        } catch (Exception e) {
            log.error("Error checking critical tickets", e);
        }
    }

    private void updatePositionsForQueue(QueueType queueType) {
        List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueType);
        
        for (int i = 0; i < waitingTickets.size(); i++) {
            Ticket ticket = waitingTickets.get(i);
            Integer newPosition = i + 1;
            Integer currentPosition = ticket.getPositionInQueue();
            boolean positionChanged = currentPosition == null || !newPosition.equals(currentPosition);
            
            ticket.setPositionInQueue(newPosition);
            ticket.setEstimatedWaitMinutes(newPosition * queueType.getAverageTimeMinutes());
            
            // RN-012: Cambiar a PROXIMO si posición <= 3
            if (newPosition <= 3 && ticket.getStatus() == TicketStatus.EN_ESPERA) {
                ticket.setStatus(TicketStatus.PROXIMO);
                messageService.scheduleProximoTurnoMessage(ticket);
                log.info("Ticket {} moved to PROXIMO status (position {})", 
                    ticket.getNumero(), newPosition);
            }
            
            if (positionChanged) {
                log.debug("Updated position for ticket {}: {} -> {}", 
                    ticket.getNumero(), 
                    String.valueOf(currentPosition), 
                    String.valueOf(newPosition));
            }
        }
        
        if (!waitingTickets.isEmpty()) {
            ticketRepository.saveAll(waitingTickets);
        }
    }

    private void cleanupOldTickets() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<Ticket> oldTickets = ticketRepository.findAll()
            .stream()
            .filter(t -> t.getCreatedAt().isBefore(cutoff))
            .filter(t -> !t.getStatus().isActive())
            .toList();

        if (!oldTickets.isEmpty()) {
            log.info("Cleaning up {} old tickets", oldTickets.size());
            // En producción: archivar en lugar de eliminar
            // ticketRepository.deleteAll(oldTickets);
        }
    }

    private void resetDailyCounters() {
        // Reset de contadores diarios de asesores
        log.info("Resetting daily counters for advisors");
        // En producción: actualizar totalTicketsServedToday = 0
    }
}