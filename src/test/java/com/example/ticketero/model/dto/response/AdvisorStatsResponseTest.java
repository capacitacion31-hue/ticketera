package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorStatsResponse Tests")
class AdvisorStatsResponseTest {

    @Test
    @DisplayName("Debe crear AdvisorStatsResponse correctamente")
    void debeCrearAdvisorStatsResponseCorrectamente() {
        // Given
        var performance = new AdvisorStatsResponse.AdvisorPerformance(
            15, 5.5, 6.0, 91.7, "Excelente"
        );
        var ticketDetail = new AdvisorStatsResponse.TicketDetail(
            "C001", "CAJA", 5, 4, "-1 min", "Mejor que estimado"
        );

        // When
        var response = new AdvisorStatsResponse(
            1L, "María López", LocalDate.now(), performance, List.of(ticketDetail)
        );

        // Then
        assertThat(response.advisorId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("María López");
        assertThat(response.performance().totalTicketsServed()).isEqualTo(15);
        assertThat(response.ticketDetails()).hasSize(1);
    }
}