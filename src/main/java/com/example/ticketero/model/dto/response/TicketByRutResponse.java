package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;

public record TicketByRutResponse(
    String nationalId,
    ActiveTicketInfo activeTicket,
    String message,
    ActiveTicketInfo lastTicketToday
) {
    public record ActiveTicketInfo(
        String numero,
        String status,
        Integer positionInQueue,
        Integer estimatedWaitMinutes,
        String queueType,
        String message,
        LocalDateTime lastUpdated
    ) {}
}