package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueStatsResponse Tests")
class QueueStatsResponseTest {

    @Test
    @DisplayName("Debe crear QueueStatsResponse correctamente")
    void debeCrearQueueStatsResponseCorrectamente() {
        // Given
        QueueType queueType = QueueType.CAJA;
        LocalDate date = LocalDate.now();
        Integer ticketsCompleted = 45;
        Integer ticketsWaiting = 8;
        Integer ticketsBeingServed = 2;
        Integer averageServiceTime = 7;
        Integer averageWaitTime = 12;
        Integer criticalTickets = 1;
        String peakHour = "14:00";
        Double efficiency = 85.5;
        Map<String, String> trends = Map.of("trend", "increasing");

        // When
        QueueStatsResponse response = new QueueStatsResponse(
            queueType, date, ticketsCompleted, ticketsWaiting, ticketsBeingServed,
            averageServiceTime, averageWaitTime, criticalTickets, peakHour, efficiency, trends
        );

        // Then
        assertThat(response.queueType()).isEqualTo(queueType);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.ticketsCompleted()).isEqualTo(ticketsCompleted);
        assertThat(response.ticketsWaiting()).isEqualTo(ticketsWaiting);
    }
}