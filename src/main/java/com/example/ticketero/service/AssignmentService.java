package com.example.ticketero.service;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.QueueStatsRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssignmentService {

    private final TicketRepository ticketRepository;
    private final AdvisorRepository advisorRepository;
    private final QueueStatsRepository queueStatsRepository;
    private final MessageService messageService;
    private final AuditService auditService;

    @Scheduled(fixedDelay = 3000) // Cada 3 segundos
    @Transactional
    public void processTicketAssignments() {
        List<Advisor> availableAdvisors = advisorRepository.findByStatus(AdvisorStatus.AVAILABLE);
        
        if (availableAdvisors.isEmpty()) {
            return;
        }

        // 1. Procesar tickets críticos primero (RN-016)
        processCriticalTickets(availableAdvisors);
        
        // 2. Procesar tickets normales por prioridad de cola (RN-002)
        processNormalTickets(availableAdvisors);
    }

    private void processCriticalTickets(List<Advisor> availableAdvisors) {
        LocalDateTime now = LocalDateTime.now();
        
        // Calcular límites de tiempo por cola
        LocalDateTime cajaLimit = now.minusMinutes(QueueType.CAJA.getMaxWaitTimeMinutes());
        LocalDateTime personalLimit = now.minusMinutes(QueueType.PERSONAL_BANKER.getMaxWaitTimeMinutes());
        LocalDateTime empresasLimit = now.minusMinutes(QueueType.EMPRESAS.getMaxWaitTimeMinutes());
        LocalDateTime gerenciaLimit = now.minusMinutes(QueueType.GERENCIA.getMaxWaitTimeMinutes());
        
        List<Ticket> criticalTickets = queueStatsRepository.findCriticalTicketsByTimeLimit(
            cajaLimit, personalLimit, empresasLimit, gerenciaLimit
        );

        for (Ticket ticket : criticalTickets) {
            Optional<Advisor> advisor = findBestAdvisorForTicket(ticket, availableAdvisors);
            if (advisor.isPresent()) {
                assignTicketToAdvisor(ticket, advisor.get());
                auditService.logTicketAssigned(ticket, advisor.get());
                log.warn("CRITICAL ticket {} assigned to {} (waited {} minutes)", 
                    ticket.getNumero(), advisor.get().getName(),
                    java.time.Duration.between(ticket.getCreatedAt(), now).toMinutes());
            }
        }
    }

    private void processNormalTickets(List<Advisor> availableAdvisors) {
        // RN-002: Procesar por prioridad de cola (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
        QueueType[] queuesByPriority = {
            QueueType.GERENCIA, 
            QueueType.EMPRESAS, 
            QueueType.PERSONAL_BANKER, 
            QueueType.CAJA
        };

        for (QueueType queueType : queuesByPriority) {
            List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueType);
            
            for (Ticket ticket : waitingTickets) {
                List<Advisor> currentlyAvailable = advisorRepository.findByStatus(AdvisorStatus.AVAILABLE);
                if (currentlyAvailable.isEmpty()) {
                    return; // No hay más asesores disponibles
                }
                
                Optional<Advisor> advisor = findBestAdvisorForTicket(ticket, currentlyAvailable);
                if (advisor.isPresent()) {
                    assignTicketToAdvisor(ticket, advisor.get());
                    auditService.logTicketAssigned(ticket, advisor.get());
                    log.info("Ticket {} assigned to {} (queue: {})", 
                        ticket.getNumero(), advisor.get().getName(), queueType);
                }
            }
        }
    }

    private Optional<Advisor> findBestAdvisorForTicket(Ticket ticket, List<Advisor> availableAdvisors) {
        // Filtrar asesores que pueden atender este tipo de cola
        List<Advisor> eligibleAdvisors = availableAdvisors.stream()
            .filter(advisor -> advisor.getQueueTypes() != null && advisor.getQueueTypes().contains(ticket.getQueueType()))
            .filter(advisor -> advisor.getStatus() == AdvisorStatus.AVAILABLE)
            .toList();

        if (eligibleAdvisors.isEmpty()) {
            return Optional.empty();
        }

        // RN-004: Seleccionar asesor con menor carga de trabajo
        return eligibleAdvisors.stream()
            .min(Comparator
                .comparingInt(Advisor::getWorkloadMinutes)
                .thenComparing(advisor -> advisor.getLastAssignedAt(), 
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            );
    }

    @Transactional
    public void assignTicketToAdvisor(Ticket ticket, Advisor advisor) {
        // Actualizar ticket
        ticket.setStatus(TicketStatus.ATENDIENDO);
        ticket.setAssignedAdvisor(advisor);
        ticket.setAssignedModuleNumber(advisor.getModuleNumber());
        ticket.setAssignedAt(LocalDateTime.now());
        ticket.setPositionInQueue(0);
        ticket.setEstimatedWaitMinutes(0);

        // Actualizar asesor
        advisor.setStatus(AdvisorStatus.BUSY);
        advisor.setAssignedTicketsCount(advisor.getAssignedTicketsCount() + 1);
        advisor.setWorkloadMinutes(advisor.getWorkloadMinutes() + ticket.getQueueType().getAverageTimeMinutes());
        advisor.setLastAssignedAt(LocalDateTime.now());

        // Guardar cambios
        ticketRepository.save(ticket);
        advisorRepository.save(advisor);

        // Programar mensaje "es tu turno"
        messageService.scheduleEsTuTurnoMessage(ticket);

        // Actualizar posiciones de tickets restantes en la cola
        updateQueuePositions(ticket.getQueueType());

        log.info("Assignment completed: {} → {} (module {})", 
            ticket.getNumero(), advisor.getName(), advisor.getModuleNumber());
    }

    @Transactional
    public void completeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getStatus() != TicketStatus.ATENDIENDO) {
            throw new IllegalStateException("Ticket is not being served: " + ticket.getNumero());
        }

        Advisor advisor = ticket.getAssignedAdvisor();
        LocalDateTime now = LocalDateTime.now();

        // Calcular tiempo real de atención
        if (ticket.getAssignedAt() != null) {
            long actualMinutes = java.time.Duration.between(ticket.getAssignedAt(), now).toMinutes();
            ticket.setActualServiceTimeMinutes((int) actualMinutes);
        }

        // Actualizar ticket
        ticket.setStatus(TicketStatus.COMPLETADO);
        ticket.setCompletedAt(now);

        // Liberar asesor
        advisor.setStatus(AdvisorStatus.AVAILABLE);
        advisor.setAssignedTicketsCount(Math.max(0, advisor.getAssignedTicketsCount() - 1));
        advisor.setWorkloadMinutes(Math.max(0, 
            advisor.getWorkloadMinutes() - ticket.getQueueType().getAverageTimeMinutes()));
        advisor.setTotalTicketsServedToday(advisor.getTotalTicketsServedToday() + 1);

        // Actualizar promedio de tiempo de servicio del asesor
        updateAdvisorAverageServiceTime(advisor, ticket.getActualServiceTimeMinutes());

        // Guardar cambios
        ticketRepository.save(ticket);
        advisorRepository.save(advisor);

        log.info("Ticket {} completed by {} in {} minutes", 
            ticket.getNumero(), advisor.getName(), ticket.getActualServiceTimeMinutes());
    }

    private void updateQueuePositions(QueueType queueType) {
        List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueType);
        
        for (int i = 0; i < waitingTickets.size(); i++) {
            Ticket ticket = waitingTickets.get(i);
            int newPosition = i + 1;
            
            // Actualizar posición y tiempo estimado
            ticket.setPositionInQueue(newPosition);
            ticket.setEstimatedWaitMinutes(newPosition * queueType.getAverageTimeMinutes());
            
            // RN-012: Enviar mensaje de pre-aviso si posición <= 3
            if (newPosition <= 3 && ticket.getStatus() == TicketStatus.EN_ESPERA) {
                ticket.setStatus(TicketStatus.PROXIMO);
                messageService.scheduleProximoTurnoMessage(ticket);
            }
        }
        
        ticketRepository.saveAll(waitingTickets);
    }

    private void updateAdvisorAverageServiceTime(Advisor advisor, Integer actualServiceTime) {
        if (actualServiceTime == null) return;
        
        // Promedio móvil simple
        int totalServed = advisor.getTotalTicketsServedToday();
        if (totalServed == 1) {
            advisor.setAverageServiceTimeMinutes(java.math.BigDecimal.valueOf(actualServiceTime));
        } else {
            double currentAvg = advisor.getAverageServiceTimeMinutes().doubleValue();
            double newAvg = ((currentAvg * (totalServed - 1)) + actualServiceTime) / totalServed;
            advisor.setAverageServiceTimeMinutes(java.math.BigDecimal.valueOf(newAvg));
        }
    }
}