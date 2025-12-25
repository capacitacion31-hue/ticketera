package com.example.ticketero.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditLog Entity Tests")
class AuditLogTest {

    @Nested
    @DisplayName("Constructor y Builder")
    class ConstructorYBuilder {

        @Test
        @DisplayName("Debe crear audit log con builder correctamente")
        void debeCrearAuditLogConBuilder() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            Map<String, Object> previousState = Map.of("status", "AVAILABLE");
            Map<String, Object> newState = Map.of("status", "BUSY");
            Map<String, Object> additionalData = Map.of("reason", "ticket_assigned");

            // When
            AuditLog auditLog = AuditLog.builder()
                .timestamp(timestamp)
                .eventType("ADVISOR_STATUS_CHANGED")
                .actor("system")
                .entityType("ADVISOR")
                .entityId("1")
                .previousState(previousState)
                .newState(newState)
                .additionalData(additionalData)
                .ipAddress("192.168.1.1")
                .userAgent("Test-Agent")
                .build();

            // Then
            assertThat(auditLog.getTimestamp()).isEqualTo(timestamp);
            assertThat(auditLog.getEventType()).isEqualTo("ADVISOR_STATUS_CHANGED");
            assertThat(auditLog.getActor()).isEqualTo("system");
            assertThat(auditLog.getEntityType()).isEqualTo("ADVISOR");
            assertThat(auditLog.getEntityId()).isEqualTo("1");
            assertThat(auditLog.getPreviousState()).isEqualTo(previousState);
            assertThat(auditLog.getNewState()).isEqualTo(newState);
            assertThat(auditLog.getAdditionalData()).isEqualTo(additionalData);
            assertThat(auditLog.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(auditLog.getUserAgent()).isEqualTo("Test-Agent");
        }

        @Test
        @DisplayName("Debe crear audit log con constructor sin argumentos")
        void debeCrearAuditLogConConstructorSinArgumentos() {
            // When
            AuditLog auditLog = new AuditLog();

            // Then
            assertThat(auditLog).isNotNull();
            assertThat(auditLog.getId()).isNull();
            assertThat(auditLog.getEventType()).isNull();
        }
    }

    @Nested
    @DisplayName("Callbacks JPA")
    class CallbacksJPA {

        @Test
        @DisplayName("Debe establecer timestamp en onCreate si es null")
        void debeEstablecerTimestampEnOnCreateSiEsNull() {
            // Given
            AuditLog auditLog = new AuditLog();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            auditLog.onCreate();

            // Then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertThat(auditLog.getTimestamp()).isBetween(before, after);
        }

        @Test
        @DisplayName("No debe sobrescribir timestamp existente en onCreate")
        void noDebeSobrescribirTimestampExistenteEnOnCreate() {
            // Given
            AuditLog auditLog = new AuditLog();
            LocalDateTime existingTimestamp = LocalDateTime.now().minusHours(1);
            auditLog.setTimestamp(existingTimestamp);

            // When
            auditLog.onCreate();

            // Then
            assertThat(auditLog.getTimestamp()).isEqualTo(existingTimestamp);
        }
    }

    @Nested
    @DisplayName("Estados JSON")
    class EstadosJSON {

        @Test
        @DisplayName("Debe manejar estados complejos")
        void debeManejarEstadosComplejos() {
            // Given
            Map<String, Object> complexPreviousState = Map.of(
                "status", "AVAILABLE",
                "assignedTickets", 0,
                "workloadMinutes", 0,
                "metadata", Map.of("lastAction", "login")
            );

            Map<String, Object> complexNewState = Map.of(
                "status", "BUSY",
                "assignedTickets", 1,
                "workloadMinutes", 15,
                "metadata", Map.of("lastAction", "ticket_assigned", "ticketId", "C01")
            );

            // When
            AuditLog auditLog = AuditLog.builder()
                .eventType("ADVISOR_STATUS_CHANGED")
                .actor("system")
                .entityType("ADVISOR")
                .entityId("1")
                .previousState(complexPreviousState)
                .newState(complexNewState)
                .build();

            // Then
            assertThat(auditLog.getPreviousState()).containsEntry("status", "AVAILABLE");
            assertThat(auditLog.getPreviousState()).containsEntry("assignedTickets", 0);
            assertThat(auditLog.getNewState()).containsEntry("status", "BUSY");
            assertThat(auditLog.getNewState()).containsEntry("assignedTickets", 1);
        }

        @Test
        @DisplayName("Debe permitir estados null")
        void debePermitirEstadosNull() {
            // When
            AuditLog auditLog = AuditLog.builder()
                .eventType("TICKET_CREATED")
                .actor("system")
                .entityType("TICKET")
                .entityId("C01")
                .previousState(null)
                .newState(Map.of("status", "EN_ESPERA"))
                .build();

            // Then
            assertThat(auditLog.getPreviousState()).isNull();
            assertThat(auditLog.getNewState()).isNotNull();
            assertThat(auditLog.getNewState()).containsEntry("status", "EN_ESPERA");
        }
    }

    @Nested
    @DisplayName("Datos Adicionales")
    class DatosAdicionales {

        @Test
        @DisplayName("Debe manejar datos adicionales")
        void debeManejarDatosAdicionales() {
            // Given
            Map<String, Object> additionalData = Map.of(
                "queueType", "CAJA",
                "priority", 1,
                "estimatedWaitTime", 5,
                "branchOffice", "Sucursal Centro"
            );

            // When
            AuditLog auditLog = AuditLog.builder()
                .eventType("TICKET_CREATED")
                .actor("totem")
                .entityType("TICKET")
                .entityId("C01")
                .additionalData(additionalData)
                .build();

            // Then
            assertThat(auditLog.getAdditionalData()).containsEntry("queueType", "CAJA");
            assertThat(auditLog.getAdditionalData()).containsEntry("priority", 1);
            assertThat(auditLog.getAdditionalData()).containsEntry("estimatedWaitTime", 5);
            assertThat(auditLog.getAdditionalData()).containsEntry("branchOffice", "Sucursal Centro");
        }
    }
}