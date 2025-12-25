package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardResponse Tests")
class DashboardResponseTest {

    @Test
    @DisplayName("Debe crear DashboardResponse correctamente")
    void debeCrearDashboardResponseCorrectamente() {
        // Given
        var summary = new DashboardResponse.DashboardSummary(
            150, 25, 8, 117, 3, 12, 8, "OPERATIONAL"
        );
        var alert = new DashboardResponse.AlertResponse(
            "CRITICAL_WAIT", "HIGH", "Tickets críticos detectados", 
            3, "Asignar más asesores", LocalDateTime.now()
        );
        var timestamp = LocalDateTime.now();

        // When
        var response = new DashboardResponse(
            timestamp, summary, List.of(), List.of(alert)
        );

        // Then
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.summary().totalTicketsToday()).isEqualTo(150);
        assertThat(response.summary().systemStatus()).isEqualTo("OPERATIONAL");
        assertThat(response.alerts()).hasSize(1);
        assertThat(response.alerts().get(0).type()).isEqualTo("CRITICAL_WAIT");
    }
}