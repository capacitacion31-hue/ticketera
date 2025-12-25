package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ticket Entity Tests")
class TicketTest {

    @Nested
    @DisplayName("Constructor y Builder")
    class ConstructorYBuilder {

        @Test
        @DisplayName("Debe crear ticket con builder correctamente")
        void debeCrearTicketConBuilder() {
            // Given
            UUID codigo = UUID.randomUUID();
            
            // When
            Ticket ticket = Ticket.builder()
                .codigoReferencia(codigo)
                .numero("C01")
                .nationalId("12345678")
                .telefono("+56912345678")
                .branchOffice("Sucursal Centro")
                .queueType(QueueType.CAJA)
                .status(TicketStatus.EN_ESPERA)
                .positionInQueue(1)
                .estimatedWaitMinutes(5)
                .build();

            // Then
            assertThat(ticket.getCodigoReferencia()).isEqualTo(codigo);
            assertThat(ticket.getNumero()).isEqualTo("C01");
            assertThat(ticket.getNationalId()).isEqualTo("12345678");
            assertThat(ticket.getTelefono()).isEqualTo("+56912345678");
            assertThat(ticket.getBranchOffice()).isEqualTo("Sucursal Centro");
            assertThat(ticket.getQueueType()).isEqualTo(QueueType.CAJA);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
            assertThat(ticket.getPositionInQueue()).isEqualTo(1);
            assertThat(ticket.getEstimatedWaitMinutes()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debe crear ticket con constructor sin argumentos")
        void debeCrearTicketConConstructorSinArgumentos() {
            // When
            Ticket ticket = new Ticket();

            // Then
            assertThat(ticket).isNotNull();
            assertThat(ticket.getId()).isNull();
            assertThat(ticket.getNumero()).isNull();
        }
    }

    @Nested
    @DisplayName("Callbacks JPA")
    class CallbacksJPA {

        @Test
        @DisplayName("Debe establecer valores por defecto en onCreate")
        void debeEstablecerValoresPorDefectoEnOnCreate() {
            // Given
            Ticket ticket = new Ticket();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            ticket.onCreate();

            // Then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertThat(ticket.getCreatedAt()).isBetween(before, after);
            assertThat(ticket.getUpdatedAt()).isBetween(before, after);
            assertThat(ticket.getCodigoReferencia()).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EN_ESPERA);
        }

        @Test
        @DisplayName("No debe sobrescribir codigoReferencia existente en onCreate")
        void noDebesobrescribirCodigoReferenciaExistenteEnOnCreate() {
            // Given
            Ticket ticket = new Ticket();
            UUID existingCode = UUID.randomUUID();
            ticket.setCodigoReferencia(existingCode);

            // When
            ticket.onCreate();

            // Then
            assertThat(ticket.getCodigoReferencia()).isEqualTo(existingCode);
        }

        @Test
        @DisplayName("Debe actualizar updatedAt en onUpdate")
        void debeActualizarUpdatedAtEnOnUpdate() {
            // Given
            Ticket ticket = new Ticket();
            ticket.onCreate();
            LocalDateTime originalUpdatedAt = ticket.getUpdatedAt();
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            ticket.onUpdate();

            // Then
            assertThat(ticket.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Asignación de Advisor")
    class AsignacionAdvisor {

        @Test
        @DisplayName("Debe permitir asignar advisor")
        void debePermitirAsignarAdvisor() {
            // Given
            Ticket ticket = new Ticket();
            Advisor advisor = Advisor.builder()
                .name("María López")
                .moduleNumber(1)
                .build();
            LocalDateTime assignedTime = LocalDateTime.now();

            // When
            ticket.setAssignedAdvisor(advisor);
            ticket.setAssignedModuleNumber(1);
            ticket.setAssignedAt(assignedTime);

            // Then
            assertThat(ticket.getAssignedAdvisor()).isEqualTo(advisor);
            assertThat(ticket.getAssignedModuleNumber()).isEqualTo(1);
            assertThat(ticket.getAssignedAt()).isEqualTo(assignedTime);
        }

        @Test
        @DisplayName("Debe permitir completar ticket")
        void debePermitirCompletarTicket() {
            // Given
            Ticket ticket = new Ticket();
            LocalDateTime completedTime = LocalDateTime.now();

            // When
            ticket.setStatus(TicketStatus.COMPLETADO);
            ticket.setCompletedAt(completedTime);
            ticket.setActualServiceTimeMinutes(15);

            // Then
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETADO);
            assertThat(ticket.getCompletedAt()).isEqualTo(completedTime);
            assertThat(ticket.getActualServiceTimeMinutes()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Estados del Ticket")
    class EstadosDelTicket {

        @Test
        @DisplayName("Debe permitir cambiar estados")
        void debePermitirCambiarEstados() {
            // Given
            Ticket ticket = new Ticket();

            // When & Then
            ticket.setStatus(TicketStatus.EN_ESPERA);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EN_ESPERA);

            ticket.setStatus(TicketStatus.PROXIMO);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PROXIMO);

            ticket.setStatus(TicketStatus.ATENDIENDO);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ATENDIENDO);

            ticket.setStatus(TicketStatus.COMPLETADO);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.COMPLETADO);
        }
    }
}