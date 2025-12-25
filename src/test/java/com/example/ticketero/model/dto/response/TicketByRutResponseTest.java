package com.example.ticketero.model.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TicketByRutResponse Tests")
class TicketByRutResponseTest {

    @Test
    @DisplayName("Debe crear TicketByRutResponse correctamente")
    void debeCrearTicketByRutResponseCorrectamente() {
        // Given
        var activeTicket = new TicketByRutResponse.ActiveTicketInfo(
            "C001", "EN_ESPERA", 3, 15, "CAJA", 
            "Tu turno llegará pronto", LocalDateTime.now()
        );
        var lastTicket = new TicketByRutResponse.ActiveTicketInfo(
            "C002", "COMPLETADO", 0, 0, "CAJA",
            "Ticket completado", LocalDateTime.now().minusHours(2)
        );

        // When
        var response = new TicketByRutResponse(
            "12345678-9", activeTicket, "Tienes un ticket activo", lastTicket
        );

        // Then
        assertThat(response.nationalId()).isEqualTo("12345678-9");
        assertThat(response.activeTicket().numero()).isEqualTo("C001");
        assertThat(response.activeTicket().status()).isEqualTo("EN_ESPERA");
        assertThat(response.message()).isEqualTo("Tienes un ticket activo");
        assertThat(response.lastTicketToday().status()).isEqualTo("COMPLETADO");
    }
}