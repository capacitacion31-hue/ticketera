package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdvisorResponse(
    Long id,
    String name,
    String email,
    AdvisorStatus status,
    Integer moduleNumber,
    Integer assignedTicketsCount,
    Integer workloadMinutes,
    BigDecimal averageServiceTimeMinutes,
    Integer totalTicketsServedToday,
    List<QueueType> queueTypes,
    LocalDateTime lastAssignedAt,
    LocalDateTime statusSince
) {}