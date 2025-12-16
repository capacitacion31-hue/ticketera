package com.example.ticketero.model.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AdvisorStatsResponse(
    Long advisorId,
    String name,
    LocalDate date,
    AdvisorPerformance performance,
    List<TicketDetail> ticketDetails
) {
    public record AdvisorPerformance(
        Integer totalTicketsServed,
        Double averageServiceTimeReal,
        Double averageServiceTimeEstimated,
        Double accuracy,
        String efficiency
    ) {}

    public record TicketDetail(
        String ticket,
        String queueType,
        Integer estimatedMinutes,
        Integer actualMinutes,
        String variance,
        String performance
    ) {}
}