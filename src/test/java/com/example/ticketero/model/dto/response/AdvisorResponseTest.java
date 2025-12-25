package com.example.ticketero.model.dto.response;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdvisorResponse Tests")
class AdvisorResponseTest {

    @Test
    @DisplayName("Debe crear AdvisorResponse correctamente")
    void debeCrearAdvisorResponseCorrectamente() {
        // Given
        Long id = 1L;
        String name = "María López";
        String email = "maria.lopez@banco.com";
        AdvisorStatus status = AdvisorStatus.AVAILABLE;
        Integer moduleNumber = 1;
        Integer assignedTicketsCount = 2;
        Integer workloadMinutes = 15;
        BigDecimal averageServiceTime = BigDecimal.valueOf(5.5);
        Integer totalTicketsToday = 10;
        List<QueueType> queueTypes = List.of(QueueType.CAJA);
        LocalDateTime lastAssignedAt = LocalDateTime.now();
        LocalDateTime statusSince = LocalDateTime.now();

        // When
        AdvisorResponse response = new AdvisorResponse(
            id, name, email, status, moduleNumber, assignedTicketsCount,
            workloadMinutes, averageServiceTime, totalTicketsToday,
            queueTypes, lastAssignedAt, statusSince
        );

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.moduleNumber()).isEqualTo(moduleNumber);
    }
}