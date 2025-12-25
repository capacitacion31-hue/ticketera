package com.example.ticketero.testutil;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.entity.*;
import com.example.ticketero.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Builder para crear datos de prueba consistentes.
 */
public class TestDataBuilder {

    // ============================================================
    // TICKETS
    // ============================================================
    
    public static Ticket.TicketBuilder ticketWaiting() {
        return Ticket.builder()
            .id(1L)
            .codigoReferencia(UUID.randomUUID())
            .numero("C01")
            .nationalId("12345678")
            .telefono("+56912345678")
            .branchOffice("Sucursal Centro")
            .queueType(QueueType.CAJA)
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(1)
            .estimatedWaitMinutes(5)
            .createdAt(LocalDateTime.now());
    }
    
    public static Ticket.TicketBuilder ticketInProgress() {
        return ticketWaiting()
            .status(TicketStatus.ATENDIENDO)
            .assignedAt(LocalDateTime.now().minusMinutes(2));
    }
    
    public static Ticket.TicketBuilder ticketCompleted() {
        return ticketInProgress()
            .status(TicketStatus.COMPLETADO)
            .completedAt(LocalDateTime.now());
    }

    // ============================================================
    // ADVISORS
    // ============================================================
    
    public static Advisor.AdvisorBuilder advisorAvailable() {
        return Advisor.builder()
            .id(1L)
            .name("María López")
            .email("maria.lopez@banco.com")
            .moduleNumber(1)
            .queueTypes(List.of(QueueType.CAJA))
            .status(AdvisorStatus.AVAILABLE)
            .averageServiceTimeMinutes(BigDecimal.valueOf(5))
            .totalTicketsServedToday(10)
            .assignedTicketsCount(0)
            .workloadMinutes(0)
            .createdAt(LocalDateTime.now())
            .lastAssignedAt(LocalDateTime.now());
    }
    
    public static Advisor.AdvisorBuilder advisorBusy() {
        return advisorAvailable()
            .status(AdvisorStatus.BUSY)
            .assignedTicketsCount(1)
            .workloadMinutes(15);
    }

    // ============================================================
    // REQUESTS
    // ============================================================
    
    public static TicketRequest validTicketRequest() {
        return new TicketRequest(
            "12345678",
            "+56912345678",
            "Sucursal Centro",
            QueueType.CAJA
        );
    }
    
    public static TicketRequest ticketRequestSinTelefono() {
        return new TicketRequest(
            "12345678",
            null,
            "Sucursal Centro",
            QueueType.CAJA
        );
    }

    public static TicketRequest ticketRequestPersonal() {
        return new TicketRequest(
            "12345678",
            "+56912345678",
            "Sucursal Centro",
            QueueType.PERSONAL_BANKER
        );
    }
}