package com.example.ticketero.service;

import com.example.ticketero.model.entity.*;
import com.example.ticketero.model.enums.*;
import com.example.ticketero.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService - Unit Tests")
class AssignmentServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private QueueStatsRepository queueStatsRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AssignmentService assignmentService;

    @Nested
    @DisplayName("assignTicketToAdvisor()")
    class AsignarTicket {

        @Test
        @DisplayName("con ticket y advisor válidos → debe asignar correctamente")
        void asignarTicket_conDatosValidos_debeAsignarCorrectamente() {
            // Given
            Ticket ticket = ticketWaiting().build();
            Advisor advisor = advisorAvailable().build();

            // When
            assignmentService.assignTicketToAdvisor(ticket, advisor);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ATENDIENDO);
            assertThat(ticket.getAssignedAdvisor()).isEqualTo(advisor);
            assertThat(ticket.getAssignedModuleNumber()).isEqualTo(1);
            assertThat(ticket.getAssignedAt()).isNotNull();
            assertThat(ticket.getPositionInQueue()).isEqualTo(0);

            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.BUSY);
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(1);
            assertThat(advisor.getWorkloadMinutes()).isEqualTo(5);

            verify(ticketRepository).save(ticket);
            verify(advisorRepository).save(advisor);
            verify(messageService).scheduleEsTuTurnoMessage(ticket);
        }

        @Test
        @DisplayName("debe actualizar posiciones de cola después de asignación")
        void asignarTicket_debeActualizarPosicionesCola() {
            // Given
            Ticket ticket = ticketWaiting().queueType(QueueType.CAJA).build();
            Advisor advisor = advisorAvailable().build();
            
            List<Ticket> waitingTickets = List.of(
                ticketWaiting().id(2L).positionInQueue(2).build(),
                ticketWaiting().id(3L).positionInQueue(3).build()
            );
            
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(waitingTickets);

            // When
            assignmentService.assignTicketToAdvisor(ticket, advisor);

            // Then
            verify(ticketRepository).findWaitingTicketsByQueue(QueueType.CAJA);
            verify(ticketRepository).saveAll(waitingTickets);
            
            // Verificar que las posiciones se actualizaron
            assertThat(waitingTickets.get(0).getPositionInQueue()).isEqualTo(1);
            assertThat(waitingTickets.get(1).getPositionInQueue()).isEqualTo(2);
        }

        @Test
        @DisplayName("debe cambiar status a PROXIMO si posición <= 3")
        void asignarTicket_debeMarcarProximoSiPosicionMenorIgual3() {
            // Given
            Ticket ticket = ticketWaiting().build();
            Advisor advisor = advisorAvailable().build();
            
            Ticket ticketProximo = ticketWaiting().id(2L).positionInQueue(3).build();
            when(ticketRepository.findWaitingTicketsByQueue(any()))
                .thenReturn(List.of(ticketProximo));

            // When
            assignmentService.assignTicketToAdvisor(ticket, advisor);

            // Then
            assertThat(ticketProximo.getStatus()).isEqualTo(TicketStatus.PROXIMO);
            verify(messageService).scheduleProximoTurnoMessage(ticketProximo);
        }
    }

    @Nested
    @DisplayName("completeTicket()")
    class CompletarTicket {

        @Test
        @DisplayName("con ticket en atención → debe completar y liberar advisor")
        void completarTicket_enAtencion_debeCompletarYLiberar() {
            // Given
            Advisor advisor = advisorBusy()
                .assignedTicketsCount(1)
                .workloadMinutes(15)
                .totalTicketsServedToday(5)
                .build();
            
            Ticket ticket = ticketInProgress()
                .id(1L)
                .assignedAdvisor(advisor)
                .assignedAt(LocalDateTime.now().minusMinutes(10))
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            assignmentService.completeTicket(1L);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETADO);
            assertThat(ticket.getCompletedAt()).isNotNull();
            assertThat(ticket.getActualServiceTimeMinutes()).isNotNull();

            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getTotalTicketsServedToday()).isEqualTo(6);

            verify(ticketRepository).save(ticket);
            verify(advisorRepository).save(advisor);
        }

        @Test
        @DisplayName("con ticket no en atención → debe lanzar IllegalStateException")
        void completarTicket_noEnAtencion_debeLanzarExcepcion() {
            // Given
            Ticket ticket = ticketWaiting().build();
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When + Then
            assertThatThrownBy(() -> assignmentService.completeTicket(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not being served");

            verify(ticketRepository, never()).save(any());
            verify(advisorRepository, never()).save(any());
        }

        @Test
        @DisplayName("ticket inexistente → debe lanzar IllegalArgumentException")
        void completarTicket_ticketInexistente_debeLanzarExcepcion() {
            // Given
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> assignmentService.completeTicket(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket not found: 999");
        }

        @Test
        @DisplayName("debe calcular tiempo real de atención correctamente")
        void completarTicket_debeCalcularTiempoReal() {
            // Given
            LocalDateTime assignedAt = LocalDateTime.now().minusMinutes(15);
            Advisor advisor = advisorBusy().build();
            Ticket ticket = ticketInProgress()
                .assignedAdvisor(advisor)
                .assignedAt(assignedAt)
                .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

            // When
            assignmentService.completeTicket(1L);

            // Then
            assertThat(ticket.getActualServiceTimeMinutes()).isGreaterThanOrEqualTo(14);
            assertThat(ticket.getActualServiceTimeMinutes()).isLessThanOrEqualTo(16);
        }
    }

    @Nested
    @DisplayName("processTicketAssignments()")
    class ProcesarAsignaciones {

        @Test
        @DisplayName("sin advisors disponibles → no debe procesar nada")
        void procesarAsignaciones_sinAdvisors_noDebeProcesar() {
            // Given
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

            // When
            assignmentService.processTicketAssignments();

            // Then
            verify(queueStatsRepository, never()).findCriticalTicketsByTimeLimit(any(), any(), any(), any());
            verify(ticketRepository, never()).findWaitingTicketsByQueue(any());
        }

        @Test
        @DisplayName("con tickets críticos → debe procesarlos primero")
        void procesarAsignaciones_conTicketsCriticos_debeProcesarPrimero() {
            // Given
            Advisor advisor = advisorAvailable().build();
            Ticket criticalTicket = ticketWaiting()
                .createdAt(LocalDateTime.now().minusMinutes(50))
                .build();

            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(List.of(advisor));
            when(queueStatsRepository.findCriticalTicketsByTimeLimit(any(), any(), any(), any()))
                .thenReturn(List.of(criticalTicket));

            // When
            assignmentService.processTicketAssignments();

            // Then
            verify(auditService).logTicketAssigned(criticalTicket, advisor);
            assertThat(criticalTicket.getStatus()).isEqualTo(TicketStatus.ATENDIENDO);
            assertThat(criticalTicket.getAssignedAdvisor()).isEqualTo(advisor);
        }

        @Test
        @DisplayName("debe procesar colas por prioridad (GERENCIA > EMPRESAS > PERSONAL > CAJA)")
        void procesarAsignaciones_debeProcesarPorPrioridad() {
            // Given
            Advisor advisor = advisorAvailable()
                .queueTypes(List.of(QueueType.GERENCIA, QueueType.CAJA))
                .build();
            
            Ticket ticketCaja = ticketWaiting().queueType(QueueType.CAJA).build();
            Ticket ticketGerencia = ticketWaiting().queueType(QueueType.GERENCIA).build();

            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(List.of(advisor));
            when(queueStatsRepository.findCriticalTicketsByTimeLimit(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            
            // Mock todas las colas en orden de prioridad
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.GERENCIA))
                .thenReturn(List.of(ticketGerencia));
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.EMPRESAS))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(List.of(ticketCaja));

            // When
            assignmentService.processTicketAssignments();

            // Then
            // GERENCIA debe procesarse antes que CAJA
            assertThat(ticketGerencia.getStatus()).isEqualTo(TicketStatus.ATENDIENDO);
            assertThat(ticketCaja.getStatus()).isEqualTo(TicketStatus.EN_ESPERA); // No asignado porque advisor ya ocupado
        }
    }

    @Nested
    @DisplayName("Selección de advisor")
    class SeleccionAdvisor {

        @Test
        @DisplayName("debe seleccionar advisor con menor carga de trabajo")
        void procesarAsignaciones_debeSeleccionarMenorCarga() {
            // Given
            Advisor advisor1 = advisorAvailable()
                .id(1L)
                .queueTypes(List.of(QueueType.CAJA))
                .workloadMinutes(20)
                .build();
            
            Advisor advisor2 = advisorAvailable()
                .id(2L)
                .queueTypes(List.of(QueueType.CAJA))
                .workloadMinutes(5)
                .build();

            Ticket ticket = ticketWaiting().queueType(QueueType.CAJA).build();

            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(List.of(advisor1, advisor2));
            when(queueStatsRepository.findCriticalTicketsByTimeLimit(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            
            // Mock todas las colas
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.GERENCIA))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.EMPRESAS))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.PERSONAL_BANKER))
                .thenReturn(Collections.emptyList());
            when(ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA))
                .thenReturn(List.of(ticket));

            // When
            assignmentService.processTicketAssignments();

            // Then - El advisor con menor carga debe ser asignado
            assertThat(ticket.getAssignedAdvisor().getId()).isEqualTo(2L);
        }
    }
}