package com.example.ticketero.repository;

import com.example.ticketero.model.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AuditLogRepository Tests")
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Nested
    @DisplayName("Query Derivadas")
    class QueryDerivadas {

        @Test
        @DisplayName("Debe encontrar audit logs por entidad")
        void debeEncontrarAuditLogsPorEntidad() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            AuditLog log1 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C01", now.minusMinutes(10));
            AuditLog log2 = createAuditLog("TICKET_ASSIGNED", "system", "TICKET", "C01", now.minusMinutes(5));
            AuditLog log3 = createAuditLog("ADVISOR_STATUS_CHANGED", "admin", "ADVISOR", "1", now.minusMinutes(3));
            
            entityManager.persistAndFlush(log1);
            entityManager.persistAndFlush(log2);
            entityManager.persistAndFlush(log3);

            // When
            List<AuditLog> found = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("TICKET", "C01");

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getEventType()).isEqualTo("TICKET_ASSIGNED");
            assertThat(found.get(1).getEventType()).isEqualTo("TICKET_CREATED");
        }

        @Test
        @DisplayName("Debe encontrar audit logs por tipo de evento")
        void debeEncontrarAuditLogsPorTipoDeEvento() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            AuditLog log1 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C01", now.minusMinutes(10));
            AuditLog log2 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C02", now.minusMinutes(5));
            AuditLog log3 = createAuditLog("ADVISOR_STATUS_CHANGED", "admin", "ADVISOR", "1", now.minusMinutes(3));
            
            entityManager.persistAndFlush(log1);
            entityManager.persistAndFlush(log2);
            entityManager.persistAndFlush(log3);

            // When
            List<AuditLog> found = auditLogRepository.findByEventTypeOrderByTimestampDesc("TICKET_CREATED");

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getEntityId()).isEqualTo("C02");
            assertThat(found.get(1).getEntityId()).isEqualTo("C01");
        }

        @Test
        @DisplayName("Debe encontrar audit logs por actor")
        void debeEncontrarAuditLogsPorActor() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            AuditLog log1 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C01", now.minusMinutes(10));
            AuditLog log2 = createAuditLog("ADVISOR_STATUS_CHANGED", "admin", "ADVISOR", "1", now.minusMinutes(5));
            AuditLog log3 = createAuditLog("TICKET_ASSIGNED", "system", "TICKET", "C02", now.minusMinutes(3));
            
            entityManager.persistAndFlush(log1);
            entityManager.persistAndFlush(log2);
            entityManager.persistAndFlush(log3);

            // When
            List<AuditLog> found = auditLogRepository.findByActorOrderByTimestampDesc("system");

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getEventType()).isEqualTo("TICKET_ASSIGNED");
            assertThat(found.get(1).getEventType()).isEqualTo("TICKET_CREATED");
        }
    }

    @Nested
    @DisplayName("Queries con Paginación")
    class QueriesConPaginacion {

        @Test
        @DisplayName("Debe encontrar audit logs por rango de fechas con paginación")
        void debeEncontrarAuditLogsPorRangoDeFechasConPaginacion() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(1);
            LocalDateTime end = LocalDateTime.now();
            
            AuditLog log1 = createAuditLog("EVENT_1", "system", "TICKET", "C01", start.plusMinutes(10));
            AuditLog log2 = createAuditLog("EVENT_2", "system", "TICKET", "C02", start.plusMinutes(20));
            AuditLog log3 = createAuditLog("EVENT_3", "system", "TICKET", "C03", start.plusMinutes(30));
            AuditLog log4 = createAuditLog("EVENT_4", "system", "TICKET", "C04", end.plusMinutes(10)); // Fuera del rango
            
            entityManager.persistAndFlush(log1);
            entityManager.persistAndFlush(log2);
            entityManager.persistAndFlush(log3);
            entityManager.persistAndFlush(log4);

            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<AuditLog> found = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);

            // Then
            assertThat(found.getContent()).hasSize(2);
            assertThat(found.getTotalElements()).isEqualTo(3);
            assertThat(found.getContent().get(0).getEventType()).isEqualTo("EVENT_3");
            assertThat(found.getContent().get(1).getEventType()).isEqualTo("EVENT_2");
        }
    }

    @Nested
    @DisplayName("Queries JPQL")
    class QueriesJPQL {

        @Test
        @DisplayName("Debe encontrar audit trail para entidad")
        void debeEncontrarAuditTrailParaEntidad() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            AuditLog log1 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C01", now.minusMinutes(30));
            AuditLog log2 = createAuditLog("TICKET_ASSIGNED", "system", "TICKET", "C01", now.minusMinutes(20));
            AuditLog log3 = createAuditLog("TICKET_COMPLETED", "system", "TICKET", "C01", now.minusMinutes(10));
            AuditLog log4 = createAuditLog("TICKET_CREATED", "system", "TICKET", "C02", now.minusMinutes(5));
            
            entityManager.persistAndFlush(log1);
            entityManager.persistAndFlush(log2);
            entityManager.persistAndFlush(log3);
            entityManager.persistAndFlush(log4);

            // When
            List<AuditLog> found = auditLogRepository.findAuditTrailForEntity("TICKET", "C01");

            // Then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getEventType()).isEqualTo("TICKET_COMPLETED");
            assertThat(found.get(1).getEventType()).isEqualTo("TICKET_ASSIGNED");
            assertThat(found.get(2).getEventType()).isEqualTo("TICKET_CREATED");
        }

        @Test
        @DisplayName("Debe retornar lista vacía para entidad inexistente")
        void debeRetornarListaVaciaParaEntidadInexistente() {
            // Given
            AuditLog log = createAuditLog("TICKET_CREATED", "system", "TICKET", "C01", LocalDateTime.now());
            entityManager.persistAndFlush(log);

            // When
            List<AuditLog> found = auditLogRepository.findAuditTrailForEntity("TICKET", "C99");

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Estados JSON")
    class EstadosJSON {

        @Test
        @DisplayName("Debe manejar estados JSON complejos")
        void debeManejarEstadosJSONComplejos() {
            // Given
            Map<String, Object> previousState = Map.of("status", "AVAILABLE", "workload", 0);
            Map<String, Object> newState = Map.of("status", "BUSY", "workload", 15);
            Map<String, Object> additionalData = Map.of("reason", "ticket_assigned", "ticketId", "C01");
            
            AuditLog log = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType("ADVISOR_STATUS_CHANGED")
                .actor("system")
                .entityType("ADVISOR")
                .entityId("1")
                .previousState(previousState)
                .newState(newState)
                .additionalData(additionalData)
                .build();
            
            entityManager.persistAndFlush(log);

            // When
            List<AuditLog> found = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("ADVISOR", "1");

            // Then
            assertThat(found).hasSize(1);
            AuditLog foundLog = found.get(0);
            assertThat(foundLog.getPreviousState()).containsEntry("status", "AVAILABLE");
            assertThat(foundLog.getNewState()).containsEntry("status", "BUSY");
            assertThat(foundLog.getAdditionalData()).containsEntry("ticketId", "C01");
        }
    }

    private AuditLog createAuditLog(String eventType, String actor, String entityType, String entityId, LocalDateTime timestamp) {
        return AuditLog.builder()
            .timestamp(timestamp)
            .eventType(eventType)
            .actor(actor)
            .entityType(entityType)
            .entityId(entityId)
            .build();
    }
}