package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditEventResponse Tests")
class AuditEventResponseTest {

    @Test
    @DisplayName("Debe crear AuditEventResponse correctamente")
    void debeCrearAuditEventResponseCorrectamente() {
        // Given
        var auditEvent = new AuditEventResponse.AuditEvent(
            1L, LocalDateTime.now(), "STATUS_CHANGE", "admin",
            Map.of("status", "BUSY"), Map.of("status", "AVAILABLE"),
            Map.of("reason", "Manual change")
        );

        // When
        var response = new AuditEventResponse(
            "ADVISOR", "1", List.of(auditEvent), 1
        );

        // Then
        assertThat(response.entityType()).isEqualTo("ADVISOR");
        assertThat(response.entityId()).isEqualTo("1");
        assertThat(response.events()).hasSize(1);
        assertThat(response.totalEvents()).isEqualTo(1);
        assertThat(response.events().get(0).eventType()).isEqualTo("STATUS_CHANGE");
    }
}