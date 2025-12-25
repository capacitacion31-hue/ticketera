package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TicketRepository Tests")
class TicketRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TicketRepository ticketRepository;

    @Nested
    @DisplayName("Query Derivadas Básicas")
    class QueryDerivadasBasicas {

        @Test
        @DisplayName("Debe encontrar ticket por código de referencia")
        void debeEncontrarTicketPorCodigoReferencia() {
            // Given
            UUID codigo = UUID.randomUUID();
            Ticket ticket = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket.setCodigoReferencia(codigo);
            entityManager.persistAndFlush(ticket);

            // When
            Optional<Ticket> found = ticketRepository.findByCodigoReferencia(codigo);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getNumero()).isEqualTo("C01");
        }

        @Test
        @DisplayName("Debe encontrar ticket por número")
        void debeEncontrarTicketPorNumero() {
            // Given
            Ticket ticket = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            entityManager.persistAndFlush(ticket);

            // When
            Optional<Ticket> found = ticketRepository.findByNumero("C01");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getNationalId()).isEqualTo("12345678");
        }

        @Test
        @DisplayName("Debe encontrar tickets activos por nationalId")
        void debeEncontrarTicketsActivosPorNationalId() {
            // Given
            List<TicketStatus> activeStatuses = List.of(TicketStatus.EN_ESPERA, TicketStatus.PROXIMO, TicketStatus.ATENDIENDO);
            
            Ticket ticket1 = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            Ticket ticket2 = createTicket("C02", "12345678", QueueType.CAJA, TicketStatus.COMPLETADO);
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);

            // When
            List<Ticket> found = ticketRepository.findByNationalIdAndStatusIn("12345678", activeStatuses);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
        }

        @Test
        @DisplayName("Debe verificar existencia de tickets activos")
        void debeVerificarExistenciaDeTicketsActivos() {
            // Given
            List<TicketStatus> activeStatuses = List.of(TicketStatus.EN_ESPERA, TicketStatus.PROXIMO);
            
            Ticket ticket = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            entityManager.persistAndFlush(ticket);

            // When
            boolean exists = ticketRepository.existsByNationalIdAndStatusIn("12345678", activeStatuses);

            // Then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Queries JPQL")
    class QueriesJPQL {

        @Test
        @DisplayName("Debe encontrar tickets en espera por cola")
        void debeEncontrarTicketsEnEsperaPorCola() {
            // Given
            Ticket ticket1 = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            Ticket ticket2 = createTicket("P01", "87654321", QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA);
            Ticket ticket3 = createTicket("C02", "11111111", QueueType.CAJA, TicketStatus.ATENDIENDO);
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);
            entityManager.persistAndFlush(ticket3);

            // When
            List<Ticket> found = ticketRepository.findWaitingTicketsByQueue(QueueType.CAJA);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getNumero()).isEqualTo("C01");
        }

        @Test
        @DisplayName("Debe contar tickets adelante en cola")
        void debeContarTicketsAdelanteEnCola() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime referenceTime = now.minusMinutes(2);
            
            // Tickets creados ANTES del tiempo de referencia (adelante en cola)
            Ticket ticket1 = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket1.setCreatedAt(now.minusMinutes(10));
            
            Ticket ticket2 = createTicket("C02", "87654321", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket2.setCreatedAt(now.minusMinutes(5));
            
            // Ticket creado DESPUÉS del tiempo de referencia (atrás en cola)
            Ticket ticket3 = createTicket("C03", "11111111", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket3.setCreatedAt(now);
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);
            entityManager.persistAndFlush(ticket3);

            // When
            long count = ticketRepository.countTicketsAheadInQueue(QueueType.CAJA, referenceTime);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Debe encontrar tickets críticos")
        void debeEncontrarTicketsCriticos() {
            // Given
            LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(30);
            
            // Ticket crítico - creado ANTES del límite de tiempo
            Ticket criticalTicket = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            criticalTicket.setCreatedAt(timeLimit.minusMinutes(10));
            
            // Ticket normal - creado DESPUÉS del límite de tiempo
            Ticket normalTicket = createTicket("C02", "87654321", QueueType.CAJA, TicketStatus.EN_ESPERA);
            normalTicket.setCreatedAt(timeLimit.plusMinutes(10));
            
            entityManager.persistAndFlush(criticalTicket);
            entityManager.persistAndFlush(normalTicket);

            // When
            List<Ticket> found = ticketRepository.findCriticalTickets(timeLimit);

            // Then
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getNumero()).isEqualTo("C01");
        }
    }

    @Nested
    @DisplayName("Conteos y Estadísticas")
    class ConteosYEstadisticas {

        @Test
        @DisplayName("Debe contar tickets por cola y estado")
        void debeContarTicketsPorColaYEstado() {
            // Given
            Ticket ticket1 = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            Ticket ticket2 = createTicket("C02", "87654321", QueueType.CAJA, TicketStatus.EN_ESPERA);
            Ticket ticket3 = createTicket("P01", "11111111", QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA);
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);
            entityManager.persistAndFlush(ticket3);

            // When
            long count = ticketRepository.countByQueueTypeAndStatus(QueueType.CAJA, TicketStatus.EN_ESPERA);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Debe encontrar tickets por estado ordenados por fecha")
        void debeEncontrarTicketsPorEstadoOrdenadosPorFecha() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            Ticket ticket1 = createTicket("C01", "12345678", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket1.setCreatedAt(now.minusMinutes(10));
            
            Ticket ticket2 = createTicket("C02", "87654321", QueueType.CAJA, TicketStatus.EN_ESPERA);
            ticket2.setCreatedAt(now.minusMinutes(5));
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);

            // When
            List<Ticket> found = ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.EN_ESPERA);

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getNumero()).isEqualTo("C01");
            assertThat(found.get(1).getNumero()).isEqualTo("C02");
        }
    }

    private Ticket createTicket(String numero, String nationalId, QueueType queueType, TicketStatus status) {
        Ticket ticket = Ticket.builder()
            .numero(numero)
            .nationalId(nationalId)
            .branchOffice("Sucursal Centro")
            .queueType(queueType)
            .status(status)
            .positionInQueue(1)
            .estimatedWaitMinutes(5)
            .build();
        
        // Establecer la fecha manualmente
        ticket.setCreatedAt(LocalDateTime.now());
        return ticket;
    }
}