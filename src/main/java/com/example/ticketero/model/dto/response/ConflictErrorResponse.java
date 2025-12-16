package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;

public record ConflictErrorResponse(
    String error,
    String mensaje,
    ActiveTicketInfo ticketActivo,
    LocalDateTime timestamp
) {
    public record ActiveTicketInfo(
        String numero,
        Integer positionInQueue,
        Integer estimatedWaitMinutes,
        String queueType
    ) {}
}