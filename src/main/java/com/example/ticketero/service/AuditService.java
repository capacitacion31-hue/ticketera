package com.example.ticketero.service;

import com.example.ticketero.model.dto.response.AuditEventResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.AuditLog;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logTicketCreated(Ticket ticket, String nationalId) {
        AuditLog auditLog = AuditLog.builder()
            .timestamp(LocalDateTime.now())
            .eventType("TICKET_CREADO")
            .actor("cliente:" + nationalId)
            .entityType("TICKET")
            .entityId(ticket.getNumero())
            .previousState(null)
            .newState(Map.of(
                "status", ticket.getStatus().name(),
                "queueType", ticket.getQueueType().name(),
                "positionInQueue", ticket.getPositionInQueue()
            ))
            .additionalData(Map.of(
                "branchOffice", ticket.getBranchOffice(),
                "estimatedWaitMinutes", ticket.getEstimatedWaitMinutes()
            ))
            .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit logged: TICKET_CREADO for {}", ticket.getNumero());
    }

    @Transactional
    public void logTicketAssigned(Ticket ticket, Advisor advisor) {
        AuditLog auditLog = AuditLog.builder()
            .timestamp(LocalDateTime.now())
            .eventType("TICKET_ASIGNADO")
            .actor("sistema:auto-assignment")
            .entityType("TICKET")
            .entityId(ticket.getNumero())
            .previousState(Map.of(
                "status", "EN_ESPERA",
                "assignedAdvisor", (Object) null
            ))
            .newState(Map.of(
                "status", "ATENDIENDO",
                "assignedAdvisor", advisor.getName(),
                "moduleNumber", advisor.getModuleNumber()
            ))
            .additionalData(Map.of(
                "advisorId", advisor.getId(),
                "assignmentReason", "AUTOMATIC",
                "queuePriority", ticket.getQueueType().getPriority()
            ))
            .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit logged: TICKET_ASIGNADO {} to {}", ticket.getNumero(), advisor.getName());
    }

    @Transactional
    public void logAdvisorStatusChanged(Advisor advisor, AdvisorStatus previousStatus, String reason) {
        AuditLog auditLog = AuditLog.builder()
            .timestamp(LocalDateTime.now())
            .eventType("ASESOR_STATUS_CHANGED")
            .actor("supervisor@banco.com") // En producción obtener del contexto
            .entityType("ADVISOR")
            .entityId(advisor.getId().toString())
            .previousState(Map.of(
                "status", previousStatus.name(),
                "workloadMinutes", advisor.getWorkloadMinutes()
            ))
            .newState(Map.of(
                "status", advisor.getStatus().name(),
                "workloadMinutes", advisor.getWorkloadMinutes()
            ))
            .additionalData(Map.of(
                "reason", reason != null ? reason : "Manual change",
                "moduleNumber", advisor.getModuleNumber()
            ))
            .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit logged: ASESOR_STATUS_CHANGED for {}", advisor.getName());
    }

    public AuditEventResponse getAuditTrail(String entityType, String entityId) {
        List<AuditLog> events = auditLogRepository.findAuditTrailForEntity(entityType, entityId);
        
        List<AuditEventResponse.AuditEvent> auditEvents = events.stream()
            .map(this::toAuditEvent)
            .toList();

        return new AuditEventResponse(
            entityType,
            entityId,
            auditEvents,
            auditEvents.size()
        );
    }

    private AuditEventResponse.AuditEvent toAuditEvent(AuditLog auditLog) {
        return new AuditEventResponse.AuditEvent(
            auditLog.getId(),
            auditLog.getTimestamp(),
            auditLog.getEventType(),
            auditLog.getActor(),
            auditLog.getPreviousState(),
            auditLog.getNewState(),
            auditLog.getAdditionalData()
        );
    }
}