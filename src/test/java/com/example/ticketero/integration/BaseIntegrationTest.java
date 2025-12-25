package com.example.ticketero.integration;

import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Provides TestContainers setup and common utilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final String DEFAULT_PHONE = "+56912345678";
    private static final String DEFAULT_BRANCH = "Sucursal Centro";
    private static final String GUEST_USER = "guest";
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_CHAT_ID = "123456789";
    private static final String TELEGRAM_MOCK_URL = "http://localhost:8089/bot";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ============================================================
    // TESTCONTAINERS
    // ============================================================

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("ticketero_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
        .withExposedPorts(5672, 15672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> GUEST_USER);
        registry.add("spring.rabbitmq.password", () -> GUEST_USER);

        // Telegram Mock (WireMock)
        registry.add("telegram.api-url", () -> TELEGRAM_MOCK_URL);
        registry.add("telegram.bot-token", () -> TEST_TOKEN);
        registry.add("telegram.chat-id", () -> TEST_CHAT_ID);
    }

    // ============================================================
    // SETUP
    // ============================================================

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Limpiar en orden correcto (FK constraints)
        jdbcTemplate.execute("DELETE FROM audit_log");
        jdbcTemplate.execute("DELETE FROM mensaje");
        jdbcTemplate.execute("DELETE FROM ticket");
        jdbcTemplate.execute("UPDATE advisor SET status = 'AVAILABLE', total_tickets_served_today = 0");
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    protected String createTicketRequest(String nationalId, String telefono, 
                                          String branchOffice, String queueType) {
        return """
            {
                "nationalId": "%s",
                "telefono": "%s",
                "branchOffice": "%s",
                "queueType": "%s"
            }
            """.formatted(nationalId, telefono, branchOffice, queueType);
    }

    protected String createTicketRequest(String nationalId, String queueType) {
        return createTicketRequest(nationalId, DEFAULT_PHONE, DEFAULT_BRANCH, queueType);
    }

    protected int countTicketsInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ticket WHERE status = ?",
            Integer.class, status);
    }

    protected int countMessages(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM mensaje WHERE estado_envio = ?",
            Integer.class, status);
    }

    protected int countAdvisorsInStatus(String status) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM advisor WHERE status = ?",
            Integer.class, status);
    }

    protected void waitForTicketProcessing(int expectedCompleted, int timeoutSeconds) {
        org.awaitility.Awaitility.await()
            .atMost(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .until(() -> countTicketsInStatus("COMPLETADO") >= expectedCompleted);
    }

    protected void setAdvisorStatus(Long advisorId, String status) {
        jdbcTemplate.update(
            "UPDATE advisor SET status = ? WHERE id = ?",
            status, advisorId);
    }
}