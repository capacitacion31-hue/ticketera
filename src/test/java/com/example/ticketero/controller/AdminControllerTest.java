package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.AdvisorStatusRequest;
import com.example.ticketero.model.dto.response.*;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.DashboardService;
import com.example.ticketero.service.QueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("AdminController - Integration Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private QueueService queueService;

    @MockBean
    private AdvisorService advisorService;

    @MockBean
    private AuditService auditService;

    @Nested
    @DisplayName("GET /api/admin/dashboard")
    class GetDashboard {

        @Test
        @DisplayName("debe retornar dashboard completo")
        void getDashboard_debeRetornarDashboard() throws Exception {
            // Given
            DashboardResponse response = new DashboardResponse(
                LocalDateTime.now(),
                new DashboardResponse.DashboardSummary(
                    50, 8, 3, 45, 2, 5, 12, "NORMAL"
                ),
                List.of(),
                List.of()
            );

            when(dashboardService.getDashboard()).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalTicketsToday").value(50))
                .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/summary")
    class GetPerformanceSummary {

        @Test
        @DisplayName("debe retornar resumen de performance")
        void getPerformanceSummary_debeRetornarResumen() throws Exception {
            // Given
            PerformanceSummaryResponse response = new PerformanceSummaryResponse(
                LocalDateTime.now().toLocalDate(),
                new PerformanceSummaryResponse.PerformanceMetrics(
                    5, 5.2, 12, 85.5, 4.2, "10:00-12:00", 100, 95.0
                ),
                Map.of("efficiency", "+5%"),
                List.of("Sistema funcionando correctamente")
            );

            when(dashboardService.getPerformanceSummary()).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.performance.efficiency").value(85.5))
                .andExpect(jsonPath("$.performance.totalCustomersServed").value(100))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0]").value("Sistema funcionando correctamente"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/queues")
    class GetAllQueues {

        @Test
        @DisplayName("debe retornar todas las colas")
        void getAllQueues_debeRetornarTodasLasColas() throws Exception {
            // Given
            List<QueueSummaryResponse> response = List.of(
                new QueueSummaryResponse(
                    QueueType.CAJA,
                    "Caja",
                    5, 1, "C", 45,
                    5, 2, 45, 12, 2,
                    "NORMAL"
                ),
                new QueueSummaryResponse(
                    QueueType.PERSONAL_BANKER,
                    "Personal Banker",
                    15, 2, "P", 60,
                    3, 1, 20, 18, 0,
                    "NORMAL"
                )
            );

            when(queueService.getAllQueuesSummary()).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/queues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].queueType").value("CAJA"))
                .andExpect(jsonPath("$[0].displayName").value("Caja"))
                .andExpect(jsonPath("$[0].ticketsWaiting").value(5))
                .andExpect(jsonPath("$[1].queueType").value("PERSONAL_BANKER"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/queues/{type}")
    class GetQueueSummary {

        @Test
        @DisplayName("con tipo válido → debe retornar resumen de cola")
        void getQueueSummary_conTipoValido_debeRetornarResumen() throws Exception {
            // Given
            QueueSummaryResponse response = new QueueSummaryResponse(
                QueueType.CAJA,
                "Caja",
                5, 1, "C", 45,
                8, 3, 50, 15, 1,
                "MEDIUM_LOAD"
            );

            when(queueService.getQueueSummary(QueueType.CAJA)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/queues/{type}", "CAJA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueType").value("CAJA"))
                .andExpect(jsonPath("$.displayName").value("Caja"))
                .andExpect(jsonPath("$.ticketsWaiting").value(8))
                .andExpect(jsonPath("$.ticketsBeingServed").value(3))
                .andExpect(jsonPath("$.status").value("MEDIUM_LOAD"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/queues/{type}/stats")
    class GetQueueStats {

        @Test
        @DisplayName("con tipo válido → debe retornar estadísticas detalladas")
        void getQueueStats_conTipoValido_debeRetornarEstadisticas() throws Exception {
            // Given
            QueueStatsResponse response = new QueueStatsResponse(
                QueueType.CAJA,
                LocalDateTime.now().toLocalDate(),
                45, 8, 3, 4, 12, 2,
                "10:00-11:00",
                88.5,
                Map.of("waitTimeVsPrevious", "+5%", "serviceTimeVsPrevious", "-2%")
            );

            when(queueService.getQueueStats(QueueType.CAJA)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/queues/{type}/stats", "CAJA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueType").value("CAJA"))
                .andExpect(jsonPath("$.ticketsCompleted").value(45))
                .andExpect(jsonPath("$.averageServiceTimeMinutes").value(4))
                .andExpect(jsonPath("$.efficiency").value(88.5))
                .andExpect(jsonPath("$.trends.waitTimeVsPrevious").value("+5%"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/advisors")
    class GetAllAdvisors {

        @Test
        @DisplayName("debe retornar lista de advisors")
        void getAllAdvisors_debeRetornarLista() throws Exception {
            // Given
            List<AdvisorResponse> response = List.of(
                new AdvisorResponse(
                    1L,
                    "María López",
                    "maria@test.com",
                    AdvisorStatus.AVAILABLE,
                    1,
                    0,
                    0,
                    java.math.BigDecimal.valueOf(5.0),
                    10,
                    List.of(QueueType.CAJA, QueueType.PERSONAL_BANKER),
                    null,
                    LocalDateTime.now()
                ),
                new AdvisorResponse(
                    2L,
                    "Juan Pérez",
                    "juan@test.com",
                    AdvisorStatus.BUSY,
                    2,
                    2,
                    30,
                    java.math.BigDecimal.valueOf(6.5),
                    8,
                    List.of(QueueType.EMPRESAS),
                    LocalDateTime.now().minusMinutes(15),
                    LocalDateTime.now()
                )
            );

            when(advisorService.getAllAdvisors()).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/advisors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("María López"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].name").value("Juan Pérez"))
                .andExpect(jsonPath("$[1].status").value("BUSY"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/advisors/{id}/stats")
    class GetAdvisorStats {

        @Test
        @DisplayName("con ID válido → debe retornar estadísticas del advisor")
        void getAdvisorStats_conIdValido_debeRetornarEstadisticas() throws Exception {
            // Given
            AdvisorStatsResponse response = new AdvisorStatsResponse(
                1L,
                "María López",
                LocalDateTime.now().toLocalDate(),
                new AdvisorStatsResponse.AdvisorPerformance(
                    15, 5.2, 5.0, 95.0, "ABOVE_AVERAGE"
                ),
                List.of(
                    new AdvisorStatsResponse.TicketDetail(
                        "C01", "CAJA", 5, 4, "-1 min", "GOOD"
                    )
                )
            );

            when(advisorService.getAdvisorStats(1L)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/advisors/{id}/stats", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advisorId").value(1))
                .andExpect(jsonPath("$.name").value("María López"))
                .andExpect(jsonPath("$.performance.totalTicketsServed").value(15))
                .andExpect(jsonPath("$.performance.efficiency").value("ABOVE_AVERAGE"))
                .andExpect(jsonPath("$.ticketDetails").isArray())
                .andExpect(jsonPath("$.ticketDetails[0].ticket").value("C01"));
        }

        @Test
        @DisplayName("con ID inválido → debe retornar 400")
        void getAdvisorStats_conIdInvalido_debeRetornar400() throws Exception {
            // Given
            when(advisorService.getAdvisorStats(999L))
                .thenThrow(new IllegalArgumentException("Advisor not found: 999"));

            // When & Then
            mockMvc.perform(get("/api/admin/advisors/{id}/stats", 999L))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/advisors/{id}/status")
    class ChangeAdvisorStatus {

        @Test
        @DisplayName("con datos válidos → debe cambiar estado")
        void changeAdvisorStatus_conDatosValidos_debeCambiarEstado() throws Exception {
            // Given
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.OFFLINE,
                "Descanso programado"
            );

            AdvisorStatusChangeResponse response = new AdvisorStatusChangeResponse(
                1L,
                "María López",
                AdvisorStatus.OFFLINE,
                AdvisorStatus.AVAILABLE,
                LocalDateTime.now(),
                "admin",
                "Descanso programado"
            );

            when(advisorService.changeStatus(eq(1L), any(AdvisorStatusRequest.class)))
                .thenReturn(response);

            // When & Then
            mockMvc.perform(put("/api/admin/advisors/{id}/status", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("María López"))
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.previousStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.reason").value("Descanso programado"));
        }

        @Test
        @DisplayName("con datos inválidos → debe retornar 400")
        void changeAdvisorStatus_conDatosInvalidos_debeRetornar400() throws Exception {
            // Given - Request sin status
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                null,
                "Test"
            );

            // When & Then
            mockMvc.perform(put("/api/admin/advisors/{id}/status", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/audit/ticket/{ticketId}")
    class GetTicketAudit {

        @Test
        @DisplayName("con ticketId válido → debe retornar auditoría")
        void getTicketAudit_conTicketIdValido_debeRetornarAuditoria() throws Exception {
            // Given
            AuditEventResponse response = new AuditEventResponse(
                "TICKET",
                "C01",
                List.of(
                    new AuditEventResponse.AuditEvent(
                        1L,
                        LocalDateTime.now(),
                        "TICKET_CREATED",
                        "12345678",
                        Map.of(),
                        Map.of("numero", "C01"),
                        Map.of("description", "Ticket C01 creado")
                    )
                ),
                1
            );

            when(auditService.getAuditTrail("TICKET", "C01")).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/audit/ticket/{ticketId}", "C01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("TICKET"))
                .andExpect(jsonPath("$.entityId").value("C01"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].eventType").value("TICKET_CREATED"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/audit/advisor/{advisorId}")
    class GetAdvisorAudit {

        @Test
        @DisplayName("con advisorId válido → debe retornar auditoría")
        void getAdvisorAudit_conAdvisorIdValido_debeRetornarAuditoria() throws Exception {
            // Given
            AuditEventResponse response = new AuditEventResponse(
                "ADVISOR",
                "1",
                List.of(
                    new AuditEventResponse.AuditEvent(
                        1L,
                        LocalDateTime.now(),
                        "STATUS_CHANGED",
                        "admin",
                        Map.of("status", "AVAILABLE"),
                        Map.of("status", "OFFLINE"),
                        Map.of("description", "Estado cambiado a OFFLINE")
                    )
                ),
                1
            );

            when(auditService.getAuditTrail("ADVISOR", "1")).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/admin/audit/advisor/{advisorId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("ADVISOR"))
                .andExpect(jsonPath("$.entityId").value("1"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].eventType").value("STATUS_CHANGED"));
        }
    }
}