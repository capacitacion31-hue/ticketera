package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;

import java.time.LocalDate;
import java.util.Map;

public record QueueStatsResponse(
    QueueType queueType,
    LocalDate date,
    Integer ticketsCompleted,
    Integer ticketsWaiting,
    Integer ticketsBeingServed,
    Integer averageServiceTimeMinutes,
    Integer averageWaitTimeMinutes,
    Integer criticalTickets,
    String peakHour,
    Double efficiency,
    Map<String, String> trends
) {}