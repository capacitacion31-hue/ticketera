package com.example.ticketero.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Suite completa de tests E2E para el Sistema Ticketero
 * 
 * Ejecuta todos los tests de integración en orden:
 * 1. Validaciones de Input
 * 2. Creación de Tickets  
 * 3. Procesamiento de Tickets
 * 4. Notificaciones Telegram
 * 5. Dashboard Administrativo
 */
@Suite
@SelectClasses({
    ValidationIT.class,
    TicketCreationIT.class,
    TicketProcessingIT.class,
    NotificationIT.class,
    AdminDashboardIT.class
})
@DisplayName("E2E Test Suite - Sistema Ticketero")
public class E2ETestSuite {
    // Test suite ejecuta automáticamente todas las clases seleccionadas
}