package com.example.ticketero.scheduler;

import com.example.ticketero.service.AssignmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketAssignmentScheduler Tests")
class TicketAssignmentSchedulerTest {

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private TicketAssignmentScheduler ticketAssignmentScheduler;

    @Nested
    @DisplayName("Procesar Asignaciones de Tickets")
    class ProcesarAsignacionesTickets {

        @Test
        @DisplayName("Debe procesar asignaciones de tickets exitosamente")
        void processTicketAssignments_debeEjecutarExitosamente() {
            // When
            assertThatCode(() -> ticketAssignmentScheduler.processTicketAssignments())
                .doesNotThrowAnyException();

            // Then
            verify(assignmentService).processTicketAssignments();
        }

        @Test
        @DisplayName("Debe manejar errores al procesar asignaciones de tickets")
        void processTicketAssignments_debeManejarErrores() {
            // Given
            doThrow(new RuntimeException("Assignment service error"))
                .when(assignmentService).processTicketAssignments();

            // When & Then
            assertThatCode(() -> ticketAssignmentScheduler.processTicketAssignments())
                .doesNotThrowAnyException();

            verify(assignmentService).processTicketAssignments();
        }

        @Test
        @DisplayName("Debe continuar funcionando después de múltiples errores")
        void processTicketAssignments_debeContinuarDespuesDeErrores() {
            // Given
            doThrow(new RuntimeException("First error"))
                .doThrow(new RuntimeException("Second error"))
                .doNothing()
                .when(assignmentService).processTicketAssignments();

            // When & Then - Primera ejecución con error
            assertThatCode(() -> ticketAssignmentScheduler.processTicketAssignments())
                .doesNotThrowAnyException();

            // Segunda ejecución con error
            assertThatCode(() -> ticketAssignmentScheduler.processTicketAssignments())
                .doesNotThrowAnyException();

            // Tercera ejecución exitosa
            assertThatCode(() -> ticketAssignmentScheduler.processTicketAssignments())
                .doesNotThrowAnyException();

            verify(assignmentService, times(3)).processTicketAssignments();
        }
    }
}