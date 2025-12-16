package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuditEventResponse(
    String entityType,
    String entityId,
    List<AuditEvent> events,
    Integer totalEvents
) {
    public record AuditEvent(
        Long id,
        LocalDateTime timestamp,
        String eventType,
        String actor,
        Map<String, Object> previousState,
        Map<String, Object> newState,
        Map<String, Object> additionalData
    ) {}
}