package com.example.ticketero.model.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PerformanceSummaryResponse(
    LocalDate date,
    PerformanceMetrics performance,
    Map<String, String> trends,
    List<String> recommendations
) {
    public record PerformanceMetrics(
        Integer averageServiceTime,
        Double averageServiceTimeReal,
        Integer averageWaitTime,
        Double efficiency,
        Double customerSatisfaction,
        String peakHours,
        Integer totalCustomersServed,
        Double serviceTimeAccuracy
    ) {}
}