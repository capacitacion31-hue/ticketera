package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueSummaryResponse Tests")
class QueueSummaryResponseTest {

    @Test
    @DisplayName("Debe crear QueueSummaryResponse correctamente")
    void debeCrearQueueSummaryResponseCorrectamente() {
        // When
        var response = new QueueSummaryResponse(
            QueueType.CAJA, "Caja", 5, 1, "C", 30,
            8, 2, 45, 12, 1, "ACTIVE"
        );

        // Then
        assertThat(response.queueType()).isEqualTo(QueueType.CAJA);
        assertThat(response.displayName()).isEqualTo("Caja");
        assertThat(response.averageTimeMinutes()).isEqualTo(5);
        assertThat(response.priority()).isEqualTo(1);
        assertThat(response.prefix()).isEqualTo("C");
        assertThat(response.ticketsWaiting()).isEqualTo(8);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }
}