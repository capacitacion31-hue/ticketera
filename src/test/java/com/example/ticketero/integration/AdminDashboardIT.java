package com.example.ticketero.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Feature: Dashboard Administrativo")
class AdminDashboardIT extends BaseIntegrationTest {

    private static final String QUEUE_TYPE_CAJA = "CAJA";

    @BeforeEach
    void setupTestData() {
        // Ensure at least one advisor exists for tests
        jdbcTemplate.execute("""
            INSERT INTO advisor (id, name, queue_type, status, total_tickets_served_today) 
            VALUES (1, 'Test Advisor', 'CAJA', 'AVAILABLE', 0) 
            ON CONFLICT (id) DO UPDATE SET 
                status = 'AVAILABLE', 
                total_tickets_served_today = 0
            """);
    }

    @Nested
    @DisplayName("Dashboard General")
    class DashboardGeneral {

        @Test
        @DisplayName("GET /admin/dashboard → estado del sistema")
        void dashboard_debeRetornarEstado() {
            // When + Then
            given()
            .when()
                .get("/admin/dashboard")
            .then()
                .statusCode(200)
                .body("timestamp", notNullValue());
        }
    }

    @Nested
    @DisplayName("Estado de Colas")
    class EstadoColas {

        @Test
        @DisplayName("GET /admin/queues/CAJA → tickets de la cola")
        void colaEspecifica_debeRetornarTickets() {
            // When + Then
            given()
            .when()
                .get("/admin/queues/" + QUEUE_TYPE_CAJA)
            .then()
                .statusCode(200)
                .body("queueType", equalTo(QUEUE_TYPE_CAJA));
        }

        @Test
        @DisplayName("GET /admin/queues/CAJA/stats → estadísticas")
        void estadisticasCola_debeRetornarMetricas() {
            given()
            .when()
                .get("/admin/queues/" + QUEUE_TYPE_CAJA + "/stats")
            .then()
                .statusCode(200)
                .body("queueType", equalTo(QUEUE_TYPE_CAJA))
                .body("waiting", greaterThanOrEqualTo(0))
                .body("completed", greaterThanOrEqualTo(0))
                .body("avgServiceTimeMinutes", greaterThan(0));
        }
    }

    @Nested
    @DisplayName("Gestión de Asesores")
    class GestionAsesores {

        @Test
        @DisplayName("PUT /admin/advisors/{id}/status → cambiar estado")
        void cambiarEstado_debeActualizar() {
            // Given - Use the test advisor we created
            Long advisorId = 1L;

            String request = """
                {
                    "status": "OFFLINE"
                }
                """;

            // When
            given()
                .contentType("application/json")
                .body(request)
            .when()
                .put("/admin/advisors/" + advisorId + "/status")
            .then()
                .statusCode(200)
                .body("newStatus", equalTo("OFFLINE"));

            // Then - Verificar en BD
            String status = jdbcTemplate.queryForObject(
                "SELECT status FROM advisor WHERE id = ?",
                String.class, advisorId);
            org.assertj.core.api.Assertions.assertThat(status).isEqualTo("OFFLINE");

            // Cleanup
            jdbcTemplate.update(
                "UPDATE advisor SET status = 'AVAILABLE' WHERE id = ?", advisorId);
        }

        @Test
        @DisplayName("GET /admin/advisors → lista de asesores")
        void listarAsesores_debeRetornarLista() {
            given()
            .when()
                .get("/admin/advisors")
            .then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)));
        }
    }
}