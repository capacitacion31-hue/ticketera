package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.EstadoEnvio;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MensajeRepository Tests")
class MensajeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Nested
    @DisplayName("Query Derivadas")
    class QueryDerivadas {

        @Test
        @DisplayName("Debe encontrar mensajes por estado de envío")
        void debeEncontrarMensajesPorEstadoDeEnvio() {
            // Given
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje mensaje1 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.PENDIENTE);
            Mensaje mensaje2 = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.ENVIADO);
            Mensaje mensaje3 = createMensaje(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, EstadoEnvio.PENDIENTE);
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findByEstadoEnvio(EstadoEnvio.PENDIENTE);

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).extracting(Mensaje::getPlantilla)
                .containsExactlyInAnyOrder(MessageTemplate.TOTEM_TICKET_CREADO, MessageTemplate.TOTEM_ES_TU_TURNO);
        }

        @Test
        @DisplayName("Debe encontrar mensajes por ticket ID")
        void debeEncontrarMensajesPorTicketId() {
            // Given
            Ticket ticket1 = createAndPersistTicket("C01");
            Ticket ticket2 = createAndPersistTicket("C02");
            
            Mensaje mensaje1 = createMensaje(ticket1, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.ENVIADO);
            Mensaje mensaje2 = createMensaje(ticket1, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.PENDIENTE);
            Mensaje mensaje3 = createMensaje(ticket2, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.ENVIADO);
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findByTicketId(ticket1.getId());

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(m -> m.getTicket().getId().equals(ticket1.getId()));
        }

        @Test
        @DisplayName("Debe encontrar mensajes por plantilla")
        void debeEncontrarMensajesPorPlantilla() {
            // Given
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje mensaje1 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.ENVIADO);
            Mensaje mensaje2 = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.PENDIENTE);
            Mensaje mensaje3 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.FALLIDO);
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findByPlantilla(MessageTemplate.TOTEM_TICKET_CREADO);

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).allMatch(m -> m.getPlantilla() == MessageTemplate.TOTEM_TICKET_CREADO);
        }
    }

    @Nested
    @DisplayName("Queries para Scheduler")
    class QueriesParaScheduler {

        @Test
        @DisplayName("Debe encontrar mensajes pendientes listos para envío")
        void debeEncontrarMensajesPendientesListosParaEnvio() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje mensaje1 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.PENDIENTE);
            mensaje1.setFechaProgramada(now.minusMinutes(5)); // Listo para envío
            
            Mensaje mensaje2 = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.PENDIENTE);
            mensaje2.setFechaProgramada(now.plusMinutes(5)); // No listo aún
            
            Mensaje mensaje3 = createMensaje(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, EstadoEnvio.ENVIADO);
            mensaje3.setFechaProgramada(now.minusMinutes(10)); // Ya enviado
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findPendingMessages(now);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
        }

        @Test
        @DisplayName("Debe encontrar mensajes fallidos para reintentar")
        void debeEncontrarMensajesFallidosParaReintentar() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje mensaje1 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.FALLIDO);
            mensaje1.setFechaProgramada(now.minusMinutes(5));
            mensaje1.setIntentos(2); // Puede reintentar
            
            Mensaje mensaje2 = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.FALLIDO);
            mensaje2.setFechaProgramada(now.minusMinutes(3));
            mensaje2.setIntentos(4); // Ya no puede reintentar
            
            Mensaje mensaje3 = createMensaje(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, EstadoEnvio.FALLIDO);
            mensaje3.setFechaProgramada(now.plusMinutes(5));
            mensaje3.setIntentos(1); // No es tiempo aún
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findRetryableMessages(now);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(found.get(0).getIntentos()).isEqualTo(2);
        }

        @Test
        @DisplayName("Debe ordenar mensajes pendientes por fecha programada")
        void debeOrdenarMensajesPendientesPorFechaProgramada() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje mensaje1 = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.PENDIENTE);
            mensaje1.setFechaProgramada(now.minusMinutes(10));
            
            Mensaje mensaje2 = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.PENDIENTE);
            mensaje2.setFechaProgramada(now.minusMinutes(5));
            
            Mensaje mensaje3 = createMensaje(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, EstadoEnvio.PENDIENTE);
            mensaje3.setFechaProgramada(now.minusMinutes(15));
            
            entityManager.persistAndFlush(mensaje1);
            entityManager.persistAndFlush(mensaje2);
            entityManager.persistAndFlush(mensaje3);

            // When
            List<Mensaje> found = mensajeRepository.findPendingMessages(now);

            // Then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getPlantilla()).isEqualTo(MessageTemplate.TOTEM_ES_TU_TURNO);
            assertThat(found.get(1).getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(found.get(2).getPlantilla()).isEqualTo(MessageTemplate.TOTEM_PROXIMO_TURNO);
        }
    }

    @Nested
    @DisplayName("Estados y Transiciones")
    class EstadosYTransiciones {

        @Test
        @DisplayName("Debe manejar diferentes estados de envío")
        void debeManejarDiferentesEstadosDeEnvio() {
            // Given
            Ticket ticket = createAndPersistTicket("C01");
            
            Mensaje pendiente = createMensaje(ticket, MessageTemplate.TOTEM_TICKET_CREADO, EstadoEnvio.PENDIENTE);
            Mensaje enviado = createMensaje(ticket, MessageTemplate.TOTEM_PROXIMO_TURNO, EstadoEnvio.ENVIADO);
            enviado.setFechaEnvio(LocalDateTime.now());
            enviado.setTelegramMessageId("12345");
            
            Mensaje fallido = createMensaje(ticket, MessageTemplate.TOTEM_ES_TU_TURNO, EstadoEnvio.FALLIDO);
            fallido.setIntentos(3);
            
            entityManager.persistAndFlush(pendiente);
            entityManager.persistAndFlush(enviado);
            entityManager.persistAndFlush(fallido);

            // When
            List<Mensaje> pendientes = mensajeRepository.findByEstadoEnvio(EstadoEnvio.PENDIENTE);
            List<Mensaje> enviados = mensajeRepository.findByEstadoEnvio(EstadoEnvio.ENVIADO);
            List<Mensaje> fallidos = mensajeRepository.findByEstadoEnvio(EstadoEnvio.FALLIDO);

            // Then
            assertThat(pendientes).hasSize(1);
            assertThat(enviados).hasSize(1);
            assertThat(fallidos).hasSize(1);
            
            assertThat(enviados.get(0).getFechaEnvio()).isNotNull();
            assertThat(enviados.get(0).getTelegramMessageId()).isEqualTo("12345");
            assertThat(fallidos.get(0).getIntentos()).isEqualTo(3);
        }
    }

    private Ticket createAndPersistTicket(String numero) {
        Ticket ticket = Ticket.builder()
            .numero(numero)
            .nationalId("12345678")
            .branchOffice("Sucursal Centro")
            .queueType(QueueType.CAJA)
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(1)
            .estimatedWaitMinutes(5)
            .createdAt(LocalDateTime.now())
            .build();
        return entityManager.persistAndFlush(ticket);
    }

    private Mensaje createMensaje(Ticket ticket, MessageTemplate plantilla, EstadoEnvio estadoEnvio) {
        return Mensaje.builder()
            .ticket(ticket)
            .plantilla(plantilla)
            .estadoEnvio(estadoEnvio)
            .fechaProgramada(LocalDateTime.now())
            .intentos(0)
            .createdAt(LocalDateTime.now())
            .build();
    }
}