package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;

public record QueueSummaryResponse(
    QueueType queueType,
    String displayName,
    Integer averageTimeMinutes,
    Integer priority,
    String prefix,
    Integer maxWaitTimeMinutes,
    Integer ticketsWaiting,
    Integer ticketsBeingServed,
    Integer totalTicketsToday,
    Integer averageWaitTimeToday,
    Integer criticalTickets,
    String status
) {}