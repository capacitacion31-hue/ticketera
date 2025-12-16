package com.example.ticketero.service;

import com.example.ticketero.exception.ActiveTicketExistsException;
import com.example.ticketero.exception.TicketNotFoundException;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketPositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.dto.response.TicketByRutResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueService queueService;
    private final AuditService auditService;
    private final MessageService messageService;

    @Transactional
    public TicketResponse create(TicketRequest request) {
        // RN-001: Validar ticket activo existente
        validateNoActiveTicket(request.nationalId());

        // Generar número de ticket
        String numero = generateTicketNumber(request.queueType());
        
        // Calcular posición en cola
        int position = calculatePosition(request.queueType());
        int estimatedWait = position * request.queueType().getAverageTimeMinutes();

        Ticket ticket = Ticket.builder()
            .codigoReferencia(UUID.randomUUID())
            .numero(numero)
            .nationalId(request.nationalId())
            .telefono(request.telefono())
            .branchOffice(request.branchOffice())
            .queueType(request.queueType())
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(position)
            .estimatedWaitMinutes(estimatedWait)
            .build();

        Ticket saved = ticketRepository.save(ticket);
        
        // Auditoría
        auditService.logTicketCreated(saved, request.nationalId());
        
        // Programar mensaje de confirmación
        messageService.scheduleTicketCreatedMessage(saved);
        
        log.info("Ticket created: {} for customer: {}", saved.getNumero(), request.nationalId());
        return toResponse(saved);
    }

    public Optional<TicketResponse> findByCodigoReferencia(UUID codigoReferencia) {
        return ticketRepository.findByCodigoReferencia(codigoReferencia)
            .map(this::toResponse);
    }

    public TicketPositionResponse getPosition(String numero) {
        Ticket ticket = ticketRepository.findByNumero(numero)
            .orElseThrow(() -> new TicketNotFoundException(numero));
        
        // Recalcular posición en tiempo real
        updatePosition(ticket);
        
        return toPositionResponse(ticket);
    }

    public TicketByRutResponse findByRut(String nationalId) {
        List<TicketStatus> activeStatuses = List.of(
            TicketStatus.EN_ESPERA, 
            TicketStatus.PROXIMO, 
            TicketStatus.ATENDIENDO
        );
        
        List<Ticket> activeTickets = ticketRepository.findByNationalIdAndStatusIn(nationalId, activeStatuses);
        
        if (activeTickets.isEmpty()) {
            return new TicketByRutResponse(
                nationalId,
                null,
                "No tienes tickets activos. Puedes crear uno nuevo en el terminal.",
                null
            );
        }
        
        Ticket activeTicket = activeTickets.get(0);
        updatePosition(activeTicket);
        
        return new TicketByRutResponse(
            nationalId,
            toActiveTicketInfo(activeTicket),
            "Tu ticket " + activeTicket.getNumero() + " está en posición " + activeTicket.getPositionInQueue(),
            null
        );
    }

    private void validateNoActiveTicket(String nationalId) {
        List<TicketStatus> activeStatuses = List.of(
            TicketStatus.EN_ESPERA, 
            TicketStatus.PROXIMO, 
            TicketStatus.ATENDIENDO
        );
        
        if (ticketRepository.existsByNationalIdAndStatusIn(nationalId, activeStatuses)) {
            List<Ticket> activeTickets = ticketRepository.findByNationalIdAndStatusIn(nationalId, activeStatuses);
            String activeTicketNumber = activeTickets.get(0).getNumero();
            throw new ActiveTicketExistsException(nationalId, activeTicketNumber);
        }
    }

    private String generateTicketNumber(QueueType queueType) {
        // Contar TODOS los tickets de hoy para esta cola (no solo EN_ESPERA)
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        long count = ticketRepository.findAll()
            .stream()
            .filter(t -> t.getQueueType() == queueType)
            .filter(t -> t.getCreatedAt().isAfter(startOfDay))
            .count() + 1;
        return queueType.getPrefix() + String.format("%02d", count);
    }

    private int calculatePosition(QueueType queueType) {
        return (int) ticketRepository.countByQueueTypeAndStatus(queueType, TicketStatus.EN_ESPERA) + 1;
    }

    private void updatePosition(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.EN_ESPERA) {
            long ahead = ticketRepository.countTicketsAheadInQueue(ticket.getQueueType(), ticket.getCreatedAt());
            ticket.setPositionInQueue((int) ahead + 1);
            ticket.setEstimatedWaitMinutes(ticket.getPositionInQueue() * ticket.getQueueType().getAverageTimeMinutes());
        } else {
            ticket.setPositionInQueue(0);
            ticket.setEstimatedWaitMinutes(0);
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getCodigoReferencia(),
            ticket.getNumero(),
            ticket.getNationalId(),
            ticket.getBranchOffice(),
            ticket.getQueueType(),
            ticket.getStatus(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : null,
            ticket.getAssignedModuleNumber(),
            ticket.getCreatedAt(),
            ticket.getAssignedAt(),
            ticket.getCompletedAt(),
            ticket.getActualServiceTimeMinutes()
        );
    }

    private TicketPositionResponse toPositionResponse(Ticket ticket) {
        String message = switch (ticket.getStatus()) {
            case EN_ESPERA -> "Tu ticket está en espera. Posición: " + ticket.getPositionInQueue();
            case PROXIMO -> "¡Pronto será tu turno! Por favor acércate a la sucursal.";
            case ATENDIENDO -> "Dirígete al módulo " + ticket.getAssignedModuleNumber() + " - Asesor: " + ticket.getAssignedAdvisor().getName();
            case COMPLETADO -> "Tu atención ha sido completada. Gracias por tu visita.";
            default -> "Estado: " + ticket.getStatus().getDescription();
        };

        return new TicketPositionResponse(
            ticket.getNumero(),
            ticket.getStatus(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getQueueType(),
            ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : null,
            ticket.getAssignedModuleNumber(),
            message,
            LocalDateTime.now()
        );
    }

    private TicketByRutResponse.ActiveTicketInfo toActiveTicketInfo(Ticket ticket) {
        return new TicketByRutResponse.ActiveTicketInfo(
            ticket.getNumero(),
            ticket.getStatus().name(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getQueueType().name(),
            "Tu ticket " + ticket.getNumero() + " está en posición " + ticket.getPositionInQueue(),
            LocalDateTime.now()
        );
    }
}