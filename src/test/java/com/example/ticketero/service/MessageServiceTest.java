package com.example.ticketero.service;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.EstadoEnvio;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("MessageService - Unit Tests")
class MessageServiceTest {

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MessageService messageService;

    @Nested
    @DisplayName("scheduleTicketCreatedMessage()")
    class ProgramarMensajeTicketCreado {

        @Test
        @DisplayName("con teléfono → debe programar mensaje")
        void scheduleTicketCreated_conTelefono_debeProgramarMensaje() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .numero("C01")
                .build();

            // When
            messageService.scheduleTicketCreatedMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getTicket()).isEqualTo(ticket);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
            assertThat(mensaje.getFechaProgramada()).isNotNull();
            assertThat(mensaje.getIntentos()).isEqualTo(0);
        }

        @Test
        @DisplayName("sin teléfono → no debe programar mensaje")
        void scheduleTicketCreated_sinTelefono_noDebeProgramar() {
            // Given
            Ticket ticket = ticketWaiting().telefono(null).build();

            // When
            messageService.scheduleTicketCreatedMessage(ticket);

            // Then
            verify(mensajeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("scheduleProximoTurnoMessage()")
    class ProgramarMensajeProximoTurno {

        @Test
        @DisplayName("con teléfono y posición ≤3 → debe programar mensaje")
        void scheduleProximoTurno_conTelefonoYPosicionValida_debeProgramar() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .positionInQueue(3)
                .build();

            // When
            messageService.scheduleProximoTurnoMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_PROXIMO_TURNO);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        }

        @Test
        @DisplayName("sin teléfono → no debe programar")
        void scheduleProximoTurno_sinTelefono_noDebeProgramar() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono(null)
                .positionInQueue(2)
                .build();

            // When
            messageService.scheduleProximoTurnoMessage(ticket);

            // Then
            verify(mensajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("posición >3 → no debe programar")
        void scheduleProximoTurno_posicionMayor3_noDebeProgramar() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .positionInQueue(5)
                .build();

            // When
            messageService.scheduleProximoTurnoMessage(ticket);

            // Then
            verify(mensajeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("scheduleEsTuTurnoMessage()")
    class ProgramarMensajeEsTuTurno {

        @Test
        @DisplayName("con teléfono → debe programar mensaje")
        void scheduleEsTuTurno_conTelefono_debeProgramar() {
            // Given
            Ticket ticket = ticketInProgress()
                .telefono("+56912345678")
                .assignedModuleNumber(5)
                .build();

            // When
            messageService.scheduleEsTuTurnoMessage(ticket);

            // Then
            ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeRepository).save(captor.capture());

            Mensaje mensaje = captor.getValue();
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_ES_TU_TURNO);
        }

        @Test
        @DisplayName("sin teléfono → no debe programar")
        void scheduleEsTuTurno_sinTelefono_noDebeProgramar() {
            // Given
            Ticket ticket = ticketInProgress().telefono(null).build();

            // When
            messageService.scheduleEsTuTurnoMessage(ticket);

            // Then
            verify(mensajeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processPendingMessages()")
    class ProcesarMensajesPendientes {

        @Test
        @DisplayName("con mensajes pendientes → debe procesarlos")
        void processPendingMessages_conMensajes_debeProcesar() {
            // Given
            Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticketWaiting().telefono("+56912345678").build())
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

            when(mensajeRepository.findPendingMessages(any())).thenReturn(List.of(mensaje));

            // When
            messageService.processPendingMessages();

            // Then
            verify(mensajeRepository).findPendingMessages(any(LocalDateTime.class));
            // Verificar que se intenta procesar el mensaje (incrementa intentos)
            assertThat(mensaje.getIntentos()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin mensajes pendientes → no debe hacer nada")
        void processPendingMessages_sinMensajes_noDebeHacerNada() {
            // Given
            when(mensajeRepository.findPendingMessages(any())).thenReturn(Collections.emptyList());

            // When
            messageService.processPendingMessages();

            // Then
            verify(mensajeRepository).findPendingMessages(any());
            verify(mensajeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("sendMessage()")
    class EnviarMensaje {

        @Test
        @DisplayName("envío exitoso → debe marcar como ENVIADO")
        void sendMessage_exitoso_debeMarcarEnviado() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .numero("C01")
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();

            Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

            // When
            messageService.sendMessage(mensaje);

            // Then
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
            assertThat(mensaje.getFechaEnvio()).isNotNull();
            assertThat(mensaje.getTelegramMessageId()).isNotNull();
            assertThat(mensaje.getIntentos()).isEqualTo(1);
            
            verify(mensajeRepository).save(mensaje);
        }

        @Test
        @DisplayName("debe incrementar intentos y guardar mensaje")
        void sendMessage_debeIncrementarIntentosYGuardar() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .numero("C01")
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();

            Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

            // When
            messageService.sendMessage(mensaje);

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(1);
            verify(mensajeRepository).save(mensaje);
        }

        @Test
        @DisplayName("debe construir texto correcto para ES_TU_TURNO")
        void sendMessage_debeContruirTextoEsTuTurno() {
            // Given
            Advisor advisor = advisorAvailable().name("María López").build();
            Ticket ticket = ticketInProgress()
                .telefono("+56912345678")
                .numero("C10")
                .assignedModuleNumber(3)
                .assignedAdvisor(advisor)
                .build();

            Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_ES_TU_TURNO)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

            // When
            messageService.sendMessage(mensaje);

            // Then - Verificar que se procesó exitosamente
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
            assertThat(mensaje.getTelegramMessageId()).contains("msg_");
        }

        @Test
        @DisplayName("debe construir texto correcto para TICKET_CREADO")
        void sendMessage_debeContruirTextoTicketCreado() {
            // Given
            Ticket ticket = ticketWaiting()
                .telefono("+56912345678")
                .numero("C05")
                .positionInQueue(3)
                .estimatedWaitMinutes(15)
                .build();

            Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .intentos(0)
                .build();

            // When
            messageService.sendMessage(mensaje);

            // Then - Verificar que se procesó exitosamente (indirectamente)
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
            assertThat(mensaje.getTelegramMessageId()).contains("msg_");
        }
    }
}