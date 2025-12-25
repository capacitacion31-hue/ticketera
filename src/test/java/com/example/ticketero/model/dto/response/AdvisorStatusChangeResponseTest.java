package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.AdvisorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorStatusChangeResponse Tests")
class AdvisorStatusChangeResponseTest {

    @Test
    @DisplayName("Debe crear AdvisorStatusChangeResponse correctamente")
    void debeCrearAdvisorStatusChangeResponseCorrectamente() {
        // Given
        var now = LocalDateTime.now();

        // When
        var response = new AdvisorStatusChangeResponse(
            1L, "María López", AdvisorStatus.AVAILABLE, 
            AdvisorStatus.BUSY, now, "admin", "Cambio manual"
        );

        // Then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("María López");
        assertThat(response.status()).isEqualTo(AdvisorStatus.AVAILABLE);
        assertThat(response.previousStatus()).isEqualTo(AdvisorStatus.BUSY);
        assertThat(response.updatedAt()).isEqualTo(now);
        assertThat(response.updatedBy()).isEqualTo("admin");
        assertThat(response.reason()).isEqualTo("Cambio manual");
    }
}