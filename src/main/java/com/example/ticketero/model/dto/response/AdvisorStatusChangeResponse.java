package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.AdvisorStatus;

import java.time.LocalDateTime;

public record AdvisorStatusChangeResponse(
    Long id,
    String name,
    AdvisorStatus status,
    AdvisorStatus previousStatus,
    LocalDateTime updatedAt,
    String updatedBy,
    String reason
) {}