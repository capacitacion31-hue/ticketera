package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketPositionResponse(
    String numero,
    TicketStatus status,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    QueueType queueType,
    String assignedAdvisor,
    Integer assignedModuleNumber,
    String message,
    LocalDateTime lastUpdated
) {}