package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.EstadoEnvio;
import com.example.ticketero.model.enums.MessageTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mensaje Entity Tests")
class MensajeTest {

    @Nested
    @DisplayName("Constructor y Builder")
    class ConstructorYBuilder {

        @Test
        @DisplayName("Debe crear mensaje con builder correctamente")
        void debeCrearMensajeConBuilder() {
            // Given
            Ticket ticket = new Ticket();
            ticket.setNumero("C01");
            LocalDateTime fechaProgramada = LocalDateTime.now().plusMinutes(5);
            LocalDateTime fechaEnvio = LocalDateTime.now();

            // When
            Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(EstadoEnvio.ENVIADO)
                .fechaProgramada(fechaProgramada)
                .fechaEnvio(fechaEnvio)
                .telegramMessageId("12345")
                .intentos(1)
                .build();

            // Then
            assertThat(mensaje.getTicket()).isEqualTo(ticket);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
            assertThat(mensaje.getFechaProgramada()).isEqualTo(fechaProgramada);
            assertThat(mensaje.getFechaEnvio()).isEqualTo(fechaEnvio);
            assertThat(mensaje.getTelegramMessageId()).isEqualTo("12345");
            assertThat(mensaje.getIntentos()).isEqualTo(1);
        }

        @Test
        @DisplayName("Debe crear mensaje con constructor sin argumentos")
        void debeCrearMensajeConConstructorSinArgumentos() {
            // When
            Mensaje mensaje = new Mensaje();

            // Then
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.getId()).isNull();
            assertThat(mensaje.getPlantilla()).isNull();
        }
    }

    @Nested
    @DisplayName("Callbacks JPA")
    class CallbacksJPA {

        @Test
        @DisplayName("Debe establecer valores por defecto en onCreate")
        void debeEstablecerValoresPorDefectoEnOnCreate() {
            // Given
            Mensaje mensaje = new Mensaje();
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // When
            mensaje.onCreate();

            // Then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertThat(mensaje.getCreatedAt()).isBetween(before, after);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        }

        @Test
        @DisplayName("No debe sobrescribir estadoEnvio existente en onCreate")
        void noDebeSobrescribirEstadoEnvioExistenteEnOnCreate() {
            // Given
            Mensaje mensaje = new Mensaje();
            mensaje.setEstadoEnvio(EstadoEnvio.ENVIADO);

            // When
            mensaje.onCreate();

            // Then
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);
        }
    }

    @Nested
    @DisplayName("Estados del Mensaje")
    class EstadosDelMensaje {

        @Test
        @DisplayName("Debe permitir cambiar estados")
        void debePermitirCambiarEstados() {
            // Given
            Mensaje mensaje = new Mensaje();

            // When & Then
            mensaje.setEstadoEnvio(EstadoEnvio.PENDIENTE);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);

            mensaje.setEstadoEnvio(EstadoEnvio.ENVIADO);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.ENVIADO);

            mensaje.setEstadoEnvio(EstadoEnvio.FALLIDO);
            assertThat(mensaje.getEstadoEnvio()).isEqualTo(EstadoEnvio.FALLIDO);
        }

        @Test
        @DisplayName("Debe manejar intentos de envío")
        void debeManejarIntentosDeEnvio() {
            // Given
            Mensaje mensaje = Mensaje.builder()
                .intentos(0)
                .build();

            // When
            mensaje.setIntentos(1);
            assertThat(mensaje.getIntentos()).isEqualTo(1);

            mensaje.setIntentos(2);
            assertThat(mensaje.getIntentos()).isEqualTo(2);

            mensaje.setIntentos(3);
            assertThat(mensaje.getIntentos()).isEqualTo(3);

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Plantillas de Mensaje")
    class PlantillasDeMensaje {

        @Test
        @DisplayName("Debe manejar diferentes plantillas")
        void debeManejarDiferentesPlantillas() {
            // Given
            Mensaje mensaje = new Mensaje();

            // When & Then
            mensaje.setPlantilla(MessageTemplate.TOTEM_TICKET_CREADO);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_TICKET_CREADO);

            mensaje.setPlantilla(MessageTemplate.TOTEM_PROXIMO_TURNO);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_PROXIMO_TURNO);

            mensaje.setPlantilla(MessageTemplate.TOTEM_ES_TU_TURNO);
            assertThat(mensaje.getPlantilla()).isEqualTo(MessageTemplate.TOTEM_ES_TU_TURNO);
        }
    }

    @Nested
    @DisplayName("Fechas y Programación")
    class FechasYProgramacion {

        @Test
        @DisplayName("Debe manejar fechas de programación y envío")
        void debeManejarFechasDeProgramacionYEnvio() {
            // Given
            LocalDateTime fechaProgramada = LocalDateTime.now().plusMinutes(10);
            LocalDateTime fechaEnvio = LocalDateTime.now();

            // When
            Mensaje mensaje = Mensaje.builder()
                .fechaProgramada(fechaProgramada)
                .fechaEnvio(fechaEnvio)
                .build();

            // Then
            assertThat(mensaje.getFechaProgramada()).isEqualTo(fechaProgramada);
            assertThat(mensaje.getFechaEnvio()).isEqualTo(fechaEnvio);
        }

        @Test
        @DisplayName("Debe permitir mensaje sin fecha de envío")
        void debePermitirMensajeSinFechaDeEnvio() {
            // Given
            LocalDateTime fechaProgramada = LocalDateTime.now().plusMinutes(5);

            // When
            Mensaje mensaje = Mensaje.builder()
                .fechaProgramada(fechaProgramada)
                .fechaEnvio(null)
                .build();

            // Then
            assertThat(mensaje.getFechaProgramada()).isEqualTo(fechaProgramada);
            assertThat(mensaje.getFechaEnvio()).isNull();
        }
    }

    @Nested
    @DisplayName("Valores por Defecto")
    class ValoresPorDefecto {

        @Test
        @DisplayName("Debe tener valores por defecto correctos")
        void debeTenerValoresPorDefectoCorrectos() {
            // When
            Mensaje mensaje = Mensaje.builder()
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .fechaProgramada(LocalDateTime.now())
                .build();

            // Then
            assertThat(mensaje.getIntentos()).isEqualTo(0);
        }
    }
}