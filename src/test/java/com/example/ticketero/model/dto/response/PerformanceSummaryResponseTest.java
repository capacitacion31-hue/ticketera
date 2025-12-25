package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceSummaryResponse Tests")
class PerformanceSummaryResponseTest {

    @Test
    @DisplayName("Debe crear PerformanceSummaryResponse correctamente")
    void debeCrearPerformanceSummaryResponseCorrectamente() {
        // Given
        var metrics = new PerformanceSummaryResponse.PerformanceMetrics(
            6, 5.8, 12, 88.5, 4.2, "14:00-16:00", 245, 92.3
        );
        var trends = Map.of("efficiency", "increasing", "waitTime", "stable");
        var recommendations = List.of("Aumentar personal en horas pico");

        // When
        var response = new PerformanceSummaryResponse(
            LocalDate.now(), metrics, trends, recommendations
        );

        // Then
        assertThat(response.date()).isEqualTo(LocalDate.now());
        assertThat(response.performance().averageServiceTime()).isEqualTo(6);
        assertThat(response.performance().efficiency()).isEqualTo(88.5);
        assertThat(response.trends()).containsEntry("efficiency", "increasing");
        assertThat(response.recommendations()).hasSize(1);
    }
}