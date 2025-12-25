package com.example.ticketero.service;

import com.example.ticketero.exception.ActiveTicketExistsException;
import com.example.ticketero.exception.TicketNotFoundException;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - Unit Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private QueueService queueService;

    @Mock
    private AuditService auditService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private TicketService ticketService;

    // ============================================================
    // CREAR TICKET
    // ============================================================
    
    @Nested
    @DisplayName("create()")
    class CrearTicket {

        @Test
        @DisplayName("con datos válidos → debe crear ticket y programar mensaje")
        void crearTicket_conDatosValidos_debeCrearTicketYProgramarMensaje() {
            // Given
            TicketRequest request = validTicketRequest();
            Ticket ticketGuardado = ticketWaiting()
                .numero("C01")
                .positionInQueue(3)
                .estimatedWaitMinutes(15)
                .build();

            when(ticketRepository.existsByNationalIdAndStatusIn(eq("12345678"), any()))
                .thenReturn(false);
            when(ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA))
                .thenReturn(2L);
            when(ticketRepository.findAll()).thenReturn(List.of());
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketGuardado);

            // When
            TicketResponse response = ticketService.create(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.numero()).isEqualTo("C01");
            assertThat(response.positionInQueue()).isEqualTo(3);
            assertThat(response.estimatedWaitMinutes()).isEqualTo(15);
            assertThat(response.status()).isEqualTo(TicketStatus.EN_ESPERA);

            // Verificar orden: primero ticket, luego auditoría y mensaje
            var inOrder = inOrder(ticketRepository, auditService, messageService);
            inOrder.verify(ticketRepository).save(any(Ticket.class));
            inOrder.verify(auditService).logTicketCreated(any(Ticket.class), eq("12345678"));
            inOrder.verify(messageService).scheduleTicketCreatedMessage(any(Ticket.class));
        }

        @Test
        @DisplayName("debe generar número de ticket correctamente")
        void crearTicket_debeGenerarNumeroCorrectamente() {
            // Given
            TicketRequest request = validTicketRequest();
            
            when(ticketRepository.existsByNationalIdAndStatusIn(any(), any())).thenReturn(false);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(0L);
            when(ticketRepository.findAll()).thenReturn(List.of());
            when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ticketService.create(request);

            // Then
            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());

            Ticket ticket = captor.getValue();
            assertThat(ticket.getNumero()).startsWith("C");
            assertThat(ticket.getQueueType()).isEqualTo(QueueType.CAJA);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
            assertThat(ticket.getCodigoReferencia()).isNotNull();
        }

        @Test
        @DisplayName("para cola PERSONAL_BANKER → debe usar prefijo P")
        void crearTicket_colaPersonal_debeUsarPrefijoP() {
            // Given
            TicketRequest request = ticketRequestPersonal();
            
            when(ticketRepository.existsByNationalIdAndStatusIn(any(), any())).thenReturn(false);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(0L);
            when(ticketRepository.findAll()).thenReturn(List.of());
            when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ticketService.create(request);

            // Then
            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getNumero()).startsWith("P");
        }

        @Test
        @DisplayName("sin teléfono → debe crear ticket igual")
        void crearTicket_sinTelefono_debeCrearTicket() {
            // Given
            TicketRequest request = ticketRequestSinTelefono();
            
            when(ticketRepository.existsByNationalIdAndStatusIn(any(), any())).thenReturn(false);
            when(ticketRepository.countByQueueTypeAndStatus(any(), any())).thenReturn(0L);
            when(ticketRepository.findAll()).thenReturn(List.of());
            when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TicketResponse response = ticketService.create(request);

            // Then
            assertThat(response).isNotNull();
            verify(messageService).scheduleTicketCreatedMessage(any());
        }

        @Test
        @DisplayName("con ticket activo existente → debe lanzar ActiveTicketExistsException")
        void crearTicket_conTicketActivo_debeLanzarExcepcion() {
            // Given
            TicketRequest request = validTicketRequest();
            List<Ticket> activeTickets = List.of(ticketWaiting().numero("C05").build());
            
            when(ticketRepository.existsByNationalIdAndStatusIn(eq("12345678"), any()))
                .thenReturn(true);
            when(ticketRepository.findByNationalIdAndStatusIn(eq("12345678"), any()))
                .thenReturn(activeTickets);

            // When + Then
            assertThatThrownBy(() -> ticketService.create(request))
                .isInstanceOf(ActiveTicketExistsException.class)
                .hasMessageContaining("12345678")
                .hasMessageContaining("C05");

            verify(ticketRepository, never()).save(any());
        }
    }

    // ============================================================
    // OBTENER TICKET
    // ============================================================
    
    @Nested
    @DisplayName("findByCodigoReferencia()")
    class ObtenerTicket {

        @Test
        @DisplayName("con UUID existente → debe retornar ticket")
        void obtenerTicket_conUuidExistente_debeRetornarTicket() {
            // Given
            UUID codigo = UUID.randomUUID();
            Ticket ticket = ticketWaiting()
                .codigoReferencia(codigo)
                .numero("C01")
                .build();

            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.of(ticket));

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isPresent();
            assertThat(response.get().identificador()).isEqualTo(codigo);
            assertThat(response.get().numero()).isEqualTo("C01");
        }

        @Test
        @DisplayName("con UUID inexistente → debe retornar Optional.empty()")
        void obtenerTicket_conUuidInexistente_debeRetornarEmpty() {
            // Given
            UUID codigo = UUID.randomUUID();
            when(ticketRepository.findByCodigoReferencia(codigo)).thenReturn(Optional.empty());

            // When
            Optional<TicketResponse> response = ticketService.findByCodigoReferencia(codigo);

            // Then
            assertThat(response).isEmpty();
        }
    }
}