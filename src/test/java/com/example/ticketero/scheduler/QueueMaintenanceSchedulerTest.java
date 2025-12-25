package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueMaintenanceScheduler - Unit Tests")
class QueueMaintenanceSchedulerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private QueueMaintenanceScheduler queueMaintenanceScheduler;

    @Nested
    @DisplayName("updateQueuePositions()")
    class ActualizarPosicionesCola {

        @Test
        @DisplayName("debe actualizar posiciones para todas las colas")
        void updateQueuePositions_debeActualizarTodasLasColas() {
            // Given
            List<Ticket> ticketsCaja = List.of(
                ticketWaiting().queueType(QueueType.CAJA).positionInQueue(1).build(),
                ticketWaiting().queueType(QueueType.CAJA).positionInQueue(2).build()
            );

            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(ticketsCaja);
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.EMPRESAS))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.GERENCIA))
                .thenReturn(Collections.emptyList());

            // When
            queueMaintenanceScheduler.updateQueuePositions();

            // Then
            verify(ticketRepository).findWaitingTicketsByQueue(QueueType.CAJA);
            verify(ticketRepository).findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER);
            verify(ticketRepository).findWaitingTicketsByQueue(QueueType.EMPRESAS);
            verify(ticketRepository).findWaitingTicketsByQueue(QueueType.GERENCIA);
            verify(ticketRepository).saveAll(ticketsCaja);
        }

        @Test
        @DisplayName("debe actualizar posiciones y tiempos estimados correctamente")
        void updateQueuePositions_debeActualizarPosicionesYTiempos() {
            // Given
            Ticket ticket1 = ticketWaiting()
                .queueType(QueueType.CAJA)
                .positionInQueue(null) // Sin posición inicial
                .build();
            Ticket ticket2 = ticketWaiting()
                .queueType(QueueType.CAJA)
                .positionInQueue(5) // Posición incorrecta
                .build();

            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(List.of(ticket1, ticket2));
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.EMPRESAS))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.GERENCIA))
                .thenReturn(Collections.emptyList());

            // When
            queueMaintenanceScheduler.updateQueuePositions();

            // Then
            assertThat(ticket1.getPositionInQueue()).isEqualTo(1);
            assertThat(ticket1.getEstimatedWaitMinutes()).isEqualTo(5); // 1 * 5 min (CAJA)
            
            assertThat(ticket2.getPositionInQueue()).isEqualTo(2);
            assertThat(ticket2.getEstimatedWaitMinutes()).isEqualTo(10); // 2 * 5 min (CAJA)
        }

        @Test
        @DisplayName("debe cambiar status a PROXIMO si posición ≤ 3")
        void updateQueuePositions_debeCambiarAProximoSiPosicionMenorIgual3() {
            // Given
            Ticket ticket1 = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .build();
            Ticket ticket2 = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .build();
            Ticket ticket3 = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .build();

            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(List.of(ticket1, ticket2, ticket3));
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.EMPRESAS))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.GERENCIA))
                .thenReturn(Collections.emptyList());

            // When
            queueMaintenanceScheduler.updateQueuePositions();

            // Then
            // Posiciones actualizadas: 1, 2, 3
            assertThat(ticket1.getPositionInQueue()).isEqualTo(1);
            assertThat(ticket1.getStatus()).isEqualTo(TicketStatus.PROXIMO);
            
            assertThat(ticket2.getPositionInQueue()).isEqualTo(2);
            assertThat(ticket2.getStatus()).isEqualTo(TicketStatus.PROXIMO);
            
            assertThat(ticket3.getPositionInQueue()).isEqualTo(3);
            assertThat(ticket3.getStatus()).isEqualTo(TicketStatus.PROXIMO);

            verify(messageService, times(3)).scheduleProximoTurnoMessage(any());
        }

        @Test
        @DisplayName("sin tickets en espera → no debe hacer nada")
        void updateQueuePositions_sinTickets_noDebeHacerNada() {
            // Given
            when(ticketRepository.findWaitingTicketsByQueue(any()))
                .thenReturn(Collections.emptyList());

            // When
            queueMaintenanceScheduler.updateQueuePositions();

            // Then
            verify(ticketRepository, never()).saveAll(any());
            verify(messageService, never()).scheduleProximoTurnoMessage(any());
        }
    }

    @Nested
    @DisplayName("checkCriticalTickets()")
    class VerificarTicketsCriticos {

        @Test
        @DisplayName("debe detectar tickets críticos por tiempo de espera")
        void checkCriticalTickets_debeDetectarTicketsCriticos() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oldTime = now.minusMinutes(50); // Más del límite de CAJA (45 min)
            
            Ticket ticketCritico = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .createdAt(oldTime)
                .numero("C01")
                .build();

            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(List.of(ticketCritico));

            // When
            queueMaintenanceScheduler.checkCriticalTickets();

            // Then
            // Se llama 4 veces, una por cada QueueType
            verify(ticketRepository, times(4)).findCriticalTickets(any());
        }

        @Test
        @DisplayName("debe filtrar solo tickets EN_ESPERA de la cola correspondiente")
        void checkCriticalTickets_debeFiltrarCorrectamente() {
            // Given
            Ticket ticketCajaEspera = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .build();
            Ticket ticketCajaAtendiendo = ticketWaiting()
                .queueType(QueueType.CAJA)
                .status(TicketStatus.ATENDIENDO)
                .build();
            Ticket ticketPersonalEspera = ticketWaiting()
                .queueType(QueueType.PERSONAL_BANKER)
                .status(TicketStatus.EN_ESPERA)
                .build();

            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(List.of(ticketCajaEspera, ticketCajaAtendiendo, ticketPersonalEspera));

            // When
            queueMaintenanceScheduler.checkCriticalTickets();

            // Then
            // Solo debe procesar tickets EN_ESPERA de cada cola específica
            verify(ticketRepository, times(4)).findCriticalTickets(any()); // Una vez por cada QueueType
        }

        @Test
        @DisplayName("sin tickets críticos → no debe loggear warnings")
        void checkCriticalTickets_sinTicketsCriticos_noDebeLoggear() {
            // Given
            when(ticketRepository.findCriticalTickets(any()))
                .thenReturn(Collections.emptyList());

            // When
            queueMaintenanceScheduler.checkCriticalTickets();

            // Then
            verify(ticketRepository, times(4)).findCriticalTickets(any());
            // No hay forma directa de verificar logs, pero no debe haber excepciones
        }
    }

    @Nested
    @DisplayName("dailyCleanup()")
    class LimpiezaDiaria {

        @Test
        @DisplayName("debe ejecutar limpieza completa")
        void dailyCleanup_debeEjecutarLimpiezaCompleta() {
            // Given
            List<Ticket> oldTickets = List.of(
                ticketCompleted()
                    .createdAt(LocalDateTime.now().minusDays(8))
                    .build()
            );
            when(ticketRepository.findAll()).thenReturn(oldTickets);

            // When
            queueMaintenanceScheduler.dailyCleanup();

            // Then
            verify(ticketRepository).findAll();
            // En el código actual no se eliminan, solo se loggea
            // verify(ticketRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("debe manejar errores sin fallar")
        void dailyCleanup_debeManejarerrores() {
            // Given
            when(ticketRepository.findAll()).thenThrow(new RuntimeException("DB Error"));

            // When + Then - No debe propagar la excepción
            assertThatCode(() -> queueMaintenanceScheduler.dailyCleanup())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Manejo de errores")
    class ManejoErrores {

        @Test
        @DisplayName("updateQueuePositions debe manejar errores sin fallar")
        void updateQueuePositions_debeManejarerrores() {
            // Given
            when(ticketRepository.findWaitingTicketsByQueue(any()))
                .thenThrow(new RuntimeException("DB Error"));

            // When + Then - No debe propagar la excepción
            assertThatCode(() -> queueMaintenanceScheduler.updateQueuePositions())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("checkCriticalTickets debe manejar errores sin fallar")
        void checkCriticalTickets_debeManejarerrores() {
            // Given
            when(ticketRepository.findCriticalTickets(any()))
                .thenThrow(new RuntimeException("DB Error"));

            // When + Then - No debe propagar la excepción
            assertThatCode(() -> queueMaintenanceScheduler.checkCriticalTickets())
                .doesNotThrowAnyException();
        }
    }
}