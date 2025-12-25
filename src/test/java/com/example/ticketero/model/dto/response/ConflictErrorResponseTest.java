package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConflictErrorResponse Tests")
class ConflictErrorResponseTest {

    @Test
    @DisplayName("Debe crear ConflictErrorResponse correctamente")
    void debeCrearConflictErrorResponseCorrectamente() {
        // Given
        var activeTicket = new ConflictErrorResponse.ActiveTicketInfo(
            "C001", 3, 15, "CAJA"
        );
        var timestamp = LocalDateTime.now();

        // When
        var response = new ConflictErrorResponse(
            "ACTIVE_TICKET_EXISTS", "Ya tienes un ticket activo", 
            activeTicket, timestamp
        );

        // Then
        assertThat(response.error()).isEqualTo("ACTIVE_TICKET_EXISTS");
        assertThat(response.mensaje()).isEqualTo("Ya tienes un ticket activo");
        assertThat(response.ticketActivo().numero()).isEqualTo("C001");
        assertThat(response.ticketActivo().positionInQueue()).isEqualTo(3);
        assertThat(response.timestamp()).isEqualTo(timestamp);
    }
}