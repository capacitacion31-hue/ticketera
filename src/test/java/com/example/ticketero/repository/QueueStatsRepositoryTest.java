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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("QueueStatsRepository Tests")
class QueueStatsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QueueStatsRepository queueStatsRepository;

    @Nested
    @DisplayName("Estadísticas Diarias")
    class EstadisticasDiarias {

        @Test
        @DisplayName("Debe contar tickets de hoy por cola y estado")
        void debeContarTicketsDeHoyPorColaYEstado() {
            // Given
            LocalDateTime today = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime yesterday = today.minusDays(1);
            
            Ticket ticketHoy1 = createTicket("C01", QueueType.CAJA, TicketStatus.COMPLETADO, today);
            Ticket ticketHoy2 = createTicket("C02", QueueType.CAJA, TicketStatus.COMPLETADO, today);
            Ticket ticketAyer = createTicket("C03", QueueType.CAJA, TicketStatus.COMPLETADO, yesterday);
            Ticket ticketOtraCola = createTicket("P01", QueueType.PERSONAL_BANKER, TicketStatus.COMPLETADO, today);
            
            entityManager.persistAndFlush(ticketHoy1);
            entityManager.persistAndFlush(ticketHoy2);
            entityManager.persistAndFlush(ticketAyer);
            entityManager.persistAndFlush(ticketOtraCola);

            // When
            long count = queueStatsRepository.countTodayByQueueAndStatus(
                QueueType.CAJA, TicketStatus.COMPLETADO, today.toLocalDate());

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Debe calcular tiempo promedio de servicio de hoy")
        void debeCalcularTiempoPromedioDeServicioDeHoy() {
            // Given
            LocalDateTime today = LocalDateTime.of(2024, 1, 15, 10, 0);
            
            Ticket ticket1 = createTicket("C01", QueueType.CAJA, TicketStatus.COMPLETADO, today);
            ticket1.setActualServiceTimeMinutes(10);
            ticket1.setCompletedAt(today);
            
            Ticket ticket2 = createTicket("C02", QueueType.CAJA, TicketStatus.COMPLETADO, today);
            ticket2.setActualServiceTimeMinutes(20);
            ticket2.setCompletedAt(today);
            
            Ticket ticket3 = createTicket("C03", QueueType.CAJA, TicketStatus.EN_ESPERA, today);
            // Sin tiempo de servicio porque no está completado
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);
            entityManager.persistAndFlush(ticket3);

            // When
            Double average = queueStatsRepository.getAverageServiceTimeToday(
                QueueType.CAJA, today.toLocalDate());

            // Then
            assertThat(average).isNotNull();
            assertThat(average).isEqualTo(15.0); // (10 + 20) / 2
        }

        @Test
        @DisplayName("Debe calcular tiempo promedio de espera de hoy")
        void debeCalcularTiempoPromedioDeEsperaDeHoy() {
            // Given
            LocalDateTime today = LocalDateTime.of(2024, 1, 15, 10, 0);
            
            Ticket ticket1 = createTicket("C01", QueueType.CAJA, TicketStatus.ATENDIENDO, today);
            ticket1.setAssignedAt(today.plusMinutes(5)); // Esperó 5 minutos
            
            Ticket ticket2 = createTicket("C02", QueueType.CAJA, TicketStatus.COMPLETADO, today);
            ticket2.setAssignedAt(today.plusMinutes(10)); // Esperó 10 minutos
            
            Ticket ticket3 = createTicket("C03", QueueType.CAJA, TicketStatus.EN_ESPERA, today);
            // Sin asignación aún
            
            entityManager.persistAndFlush(ticket1);
            entityManager.persistAndFlush(ticket2);
            entityManager.persistAndFlush(ticket3);

            // When
            Double average = queueStatsRepository.getAverageWaitTimeToday(
                QueueType.CAJA, today.toLocalDate());

            // Then
            assertThat(average).isNotNull();
            assertThat(average).isEqualTo(7.5); // (5 + 10) / 2
        }
    }

    @Nested
    @DisplayName("Tickets Críticos")
    class TicketsCriticos {

        @Test
        @DisplayName("Debe encontrar tickets críticos por tiempo límite")
        void debeEncontrarTicketsCriticosPorTiempoLimite() {
            // Given
            LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime cajaLimit = now.minusMinutes(45);
            LocalDateTime personalLimit = now.minusMinutes(60);
            LocalDateTime empresasLimit = now.minusMinutes(75);
            LocalDateTime gerenciaLimit = now.minusMinutes(90);
            
            // Tickets críticos - creados ANTES del límite
            Ticket cajaCritico = createTicket("C01", QueueType.CAJA, TicketStatus.EN_ESPERA, now.minusMinutes(50));
            Ticket personalCritico = createTicket("P01", QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA, now.minusMinutes(65));
            
            // Tickets normales - creados DESPUÉS del límite
            Ticket cajaNormal = createTicket("C02", QueueType.CAJA, TicketStatus.EN_ESPERA, now.minusMinutes(30));
            Ticket personalNormal = createTicket("P02", QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA, now.minusMinutes(40));
            
            // Ticket completado (no crítico)
            Ticket completado = createTicket("C03", QueueType.CAJA, TicketStatus.COMPLETADO, now.minusMinutes(60));
            
            entityManager.persistAndFlush(cajaCritico);
            entityManager.persistAndFlush(personalCritico);
            entityManager.persistAndFlush(cajaNormal);
            entityManager.persistAndFlush(personalNormal);
            entityManager.persistAndFlush(completado);

            // When
            List<Ticket> found = queueStatsRepository.findCriticalTicketsByTimeLimit(
                cajaLimit, personalLimit, empresasLimit, gerenciaLimit
            );

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).extracting(Ticket::getNumero)
                .containsExactlyInAnyOrder("C01", "P01");
        }

        @Test
        @DisplayName("Debe ordenar tickets críticos por prioridad de cola")
        void debeOrdenarTicketsCriticosPorPrioridadDeCola() {
            // Given
            LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime cajaLimit = now.minusMinutes(45);
            LocalDateTime personalLimit = now.minusMinutes(60);
            LocalDateTime empresasLimit = now.minusMinutes(75);
            LocalDateTime gerenciaLimit = now.minusMinutes(90);
            
            // Crear tickets críticos en diferentes colas - todos creados ANTES de sus límites
            Ticket caja = createTicket("C01", QueueType.CAJA, TicketStatus.EN_ESPERA, now.minusMinutes(50));
            Ticket personal = createTicket("P01", QueueType.PERSONAL_BANKER, TicketStatus.EN_ESPERA, now.minusMinutes(65));
            Ticket empresas = createTicket("E01", QueueType.EMPRESAS, TicketStatus.EN_ESPERA, now.minusMinutes(80));
            Ticket gerencia = createTicket("G01", QueueType.GERENCIA, TicketStatus.EN_ESPERA, now.minusMinutes(95));
            
            entityManager.persistAndFlush(caja);
            entityManager.persistAndFlush(personal);
            entityManager.persistAndFlush(empresas);
            entityManager.persistAndFlush(gerencia);

            // When
            List<Ticket> found = queueStatsRepository.findCriticalTicketsByTimeLimit(
                cajaLimit, personalLimit, empresasLimit, gerenciaLimit
            );

            // Then
            assertThat(found).hasSize(4);
            // Debe estar ordenado por prioridad DESC (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
            assertThat(found.get(0).getQueueType()).isEqualTo(QueueType.GERENCIA);
            assertThat(found.get(1).getQueueType()).isEqualTo(QueueType.EMPRESAS);
            assertThat(found.get(2).getQueueType()).isEqualTo(QueueType.PERSONAL_BANKER);
            assertThat(found.get(3).getQueueType()).isEqualTo(QueueType.CAJA);
        }

        @Test
        @DisplayName("Debe manejar caso sin tickets críticos")
        void debeManejarCasoSinTicketsCriticos() {
            // Given
            LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime cajaLimit = now.minusMinutes(45);
            LocalDateTime personalLimit = now.minusMinutes(60);
            LocalDateTime empresasLimit = now.minusMinutes(75);
            LocalDateTime gerenciaLimit = now.minusMinutes(90);
            
            // Crear solo tickets normales
            Ticket cajaNormal = createTicket("C01", QueueType.CAJA, TicketStatus.EN_ESPERA, cajaLimit.plusMinutes(10));
            entityManager.persistAndFlush(cajaNormal);

            // When
            List<Ticket> found = queueStatsRepository.findCriticalTicketsByTimeLimit(
                cajaLimit, personalLimit, empresasLimit, gerenciaLimit
            );

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Casos Edge")
    class CasosEdge {

        @Test
        @DisplayName("Debe manejar valores null en estadísticas")
        void debeManejarValoresNullEnEstadisticas() {
            // Given - No hay tickets completados hoy
            LocalDateTime today = LocalDateTime.of(2024, 1, 15, 10, 0);
            Ticket ticket = createTicket("C01", QueueType.CAJA, TicketStatus.EN_ESPERA, today);
            entityManager.persistAndFlush(ticket);

            // When
            Double averageService = queueStatsRepository.getAverageServiceTimeToday(
                QueueType.CAJA, today.toLocalDate());
            Double averageWait = queueStatsRepository.getAverageWaitTimeToday(
                QueueType.CAJA, today.toLocalDate());

            // Then
            assertThat(averageService).isNull();
            assertThat(averageWait).isNull();
        }
    }

    private Ticket createTicket(String numero, QueueType queueType, TicketStatus status, LocalDateTime createdAt) {
        Ticket ticket = Ticket.builder()
            .numero(numero)
            .nationalId("12345678")
            .branchOffice("Sucursal Centro")
            .queueType(queueType)
            .status(status)
            .positionInQueue(1)
            .estimatedWaitMinutes(queueType.getAverageTimeMinutes())
            .build();
        
        // Establecer la fecha manualmente después de la construcción
        ticket.setCreatedAt(createdAt);
        return ticket;
    }
}