package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
    LocalDateTime timestamp,
    DashboardSummary summary,
    List<QueueSummaryResponse> queuesSummary,
    List<AlertResponse> alerts
) {
    public record DashboardSummary(
        Integer totalTicketsToday,
        Integer ticketsWaiting,
        Integer ticketsBeingServed,
        Integer ticketsCompleted,
        Integer criticalTickets,
        Integer activeAdvisors,
        Integer averageWaitTime,
        String systemStatus
    ) {}

    public record AlertResponse(
        String type,
        String severity,
        String message,
        Integer count,
        String recommendedAction,
        LocalDateTime createdAt
    ) {}
}