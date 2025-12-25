package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TicketPositionResponse Tests")
class TicketPositionResponseTest {

    @Test
    @DisplayName("Debe crear TicketPositionResponse correctamente")
    void debeCrearTicketPositionResponseCorrectamente() {
        // Given
        var lastUpdated = LocalDateTime.now();

        // When
        var response = new TicketPositionResponse(
            "C001", TicketStatus.EN_ESPERA, 3, 15, QueueType.CAJA,
            "María López", 1, "Tu turno llegará pronto", lastUpdated
        );

        // Then
        assertThat(response.numero()).isEqualTo("C001");
        assertThat(response.status()).isEqualTo(TicketStatus.EN_ESPERA);
        assertThat(response.positionInQueue()).isEqualTo(3);
        assertThat(response.estimatedWaitMinutes()).isEqualTo(15);
        assertThat(response.queueType()).isEqualTo(QueueType.CAJA);
        assertThat(response.assignedAdvisor()).isEqualTo("María López");
        assertThat(response.assignedModuleNumber()).isEqualTo(1);
        assertThat(response.message()).isEqualTo("Tu turno llegará pronto");
        assertThat(response.lastUpdated()).isEqualTo(lastUpdated);
    }
}