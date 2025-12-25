package com.example.ticketero.scheduler;

import com.example.ticketero.service.MessageService;
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
@DisplayName("MessageScheduler Tests")
class MessageSchedulerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageScheduler messageScheduler;

    @Nested
    @DisplayName("Procesar Mensajes Pendientes")
    class ProcesarMensajesPendientes {

        @Test
        @DisplayName("Debe procesar mensajes pendientes exitosamente")
        void processPendingMessages_debeEjecutarExitosamente() {
            // When
            assertThatCode(() -> messageScheduler.processPendingMessages())
                .doesNotThrowAnyException();

            // Then
            verify(messageService).processPendingMessages();
        }

        @Test
        @DisplayName("Debe manejar errores al procesar mensajes pendientes")
        void processPendingMessages_debeManejarErrores() {
            // Given
            doThrow(new RuntimeException("Service error"))
                .when(messageService).processPendingMessages();

            // When & Then
            assertThatCode(() -> messageScheduler.processPendingMessages())
                .doesNotThrowAnyException();

            verify(messageService).processPendingMessages();
        }
    }

    @Nested
    @DisplayName("Procesar Mensajes de Reintento")
    class ProcesarMensajesReintento {

        @Test
        @DisplayName("Debe procesar mensajes de reintento exitosamente")
        void processRetryMessages_debeEjecutarExitosamente() {
            // When
            assertThatCode(() -> messageScheduler.processRetryMessages())
                .doesNotThrowAnyException();

            // Then
            verify(messageService).processRetryMessages();
        }

        @Test
        @DisplayName("Debe manejar errores al procesar mensajes de reintento")
        void processRetryMessages_debeManejarErrores() {
            // Given
            doThrow(new RuntimeException("Service error"))
                .when(messageService).processRetryMessages();

            // When & Then
            assertThatCode(() -> messageScheduler.processRetryMessages())
                .doesNotThrowAnyException();

            verify(messageService).processRetryMessages();
        }
    }
}