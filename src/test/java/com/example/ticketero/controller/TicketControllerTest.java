package com.example.ticketero.controller;

import com.example.ticketero.exception.ActiveTicketExistsException;
import com.example.ticketero.exception.TicketNotFoundException;
import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketByRutResponse;
import com.example.ticketero.model.dto.response.TicketPositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@DisplayName("TicketController - Integration Tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @Nested
    @DisplayName("POST /api/tickets")
    class CreateTicket {

        @Test
        @DisplayName("con datos válidos → debe crear ticket y retornar 201")
        void createTicket_conDatosValidos_debeRetornar201() throws Exception {
            // Given
            TicketRequest request = new TicketRequest(
                "12345678",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            TicketResponse response = new TicketResponse(
                UUID.randomUUID(),
                "C01",
                "12345678",
                "Sucursal Centro",
                QueueType.CAJA,
                TicketStatus.EN_ESPERA,
                1,
                5,
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null
            );

            when(ticketService.create(any(TicketRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C01"))
                .andExpect(jsonPath("$.nationalId").value("12345678"))
                .andExpect(jsonPath("$.queueType").value("CAJA"))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"))
                .andExpect(jsonPath("$.positionInQueue").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(5));
        }

        @Test
        @DisplayName("con datos inválidos → debe retornar 400")
        void createTicket_conDatosInvalidos_debeRetornar400() throws Exception {
            // Given - Request sin nationalId
            TicketRequest request = new TicketRequest(
                null,
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            // When & Then
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("con ticket activo existente → debe retornar 409")
        void createTicket_conTicketActivo_debeRetornar409() throws Exception {
            // Given
            TicketRequest request = new TicketRequest(
                "12345678",
                "+56912345678",
                "Sucursal Centro",
                QueueType.CAJA
            );

            when(ticketService.create(any(TicketRequest.class)))
                .thenThrow(new ActiveTicketExistsException("12345678", "C05"));

            // When & Then
            mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/{uuid}")
    class GetTicketByUuid {

        @Test
        @DisplayName("con UUID existente → debe retornar ticket")
        void getTicket_conUuidExistente_debeRetornarTicket() throws Exception {
            // Given
            UUID uuid = UUID.randomUUID();
            TicketResponse response = new TicketResponse(
                uuid,
                "C01",
                "12345678",
                "Sucursal Centro",
                QueueType.CAJA,
                TicketStatus.EN_ESPERA,
                1,
                5,
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null
            );

            when(ticketService.findByCodigoReferencia(uuid)).thenReturn(Optional.of(response));

            // When & Then
            mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value(uuid.toString()))
                .andExpect(jsonPath("$.numero").value("C01"));
        }

        @Test
        @DisplayName("con UUID inexistente → debe retornar 404")
        void getTicket_conUuidInexistente_debeRetornar404() throws Exception {
            // Given
            UUID uuid = UUID.randomUUID();
            when(ticketService.findByCodigoReferencia(uuid)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/tickets/{uuid}", uuid))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/{numero}/position")
    class GetTicketPosition {

        @Test
        @DisplayName("con número existente → debe retornar posición")
        void getPosition_conNumeroExistente_debeRetornarPosicion() throws Exception {
            // Given
            String numero = "C01";
            TicketPositionResponse response = new TicketPositionResponse(
                numero,
                TicketStatus.EN_ESPERA,
                3,
                15,
                QueueType.CAJA,
                null,
                null,
                "Tu ticket está en espera. Posición: 3",
                LocalDateTime.now()
            );

            when(ticketService.getPosition(numero)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/tickets/{numero}/position", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(numero))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"))
                .andExpect(jsonPath("$.positionInQueue").value(3))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(15))
                .andExpect(jsonPath("$.message").value("Tu ticket está en espera. Posición: 3"));
        }

        @Test
        @DisplayName("con número inexistente → debe retornar 404")
        void getPosition_conNumeroInexistente_debeRetornar404() throws Exception {
            // Given
            String numero = "C99";
            when(ticketService.getPosition(numero))
                .thenThrow(new TicketNotFoundException(numero));

            // When & Then
            mockMvc.perform(get("/api/tickets/{numero}/position", numero))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/by-rut/{nationalId}")
    class GetTicketByRut {

        @Test
        @DisplayName("con RUT con ticket activo → debe retornar información")
        void getByRut_conTicketActivo_debeRetornarInfo() throws Exception {
            // Given
            String nationalId = "12345678";
            TicketByRutResponse.ActiveTicketInfo ticketInfo = new TicketByRutResponse.ActiveTicketInfo(
                "C01",
                "EN_ESPERA",
                2,
                10,
                "CAJA",
                "Tu ticket C01 está en posición 2",
                LocalDateTime.now()
            );

            TicketByRutResponse response = new TicketByRutResponse(
                nationalId,
                ticketInfo,
                "Tu ticket C01 está en posición 2",
                null
            );

            when(ticketService.findByRut(nationalId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/tickets/by-rut/{nationalId}", nationalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").value(nationalId))
                .andExpect(jsonPath("$.activeTicket.numero").value("C01"))
                .andExpect(jsonPath("$.activeTicket.positionInQueue").value(2))
                .andExpect(jsonPath("$.message").value("Tu ticket C01 está en posición 2"));
        }

        @Test
        @DisplayName("con RUT sin ticket activo → debe retornar mensaje informativo")
        void getByRut_sinTicketActivo_debeRetornarMensaje() throws Exception {
            // Given
            String nationalId = "87654321";
            TicketByRutResponse response = new TicketByRutResponse(
                nationalId,
                null,
                "No tienes tickets activos. Puedes crear uno nuevo en el terminal.",
                null
            );

            when(ticketService.findByRut(nationalId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/tickets/by-rut/{nationalId}", nationalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").value(nationalId))
                .andExpect(jsonPath("$.activeTicket").doesNotExist())
                .andExpect(jsonPath("$.message").value("No tienes tickets activos. Puedes crear uno nuevo en el terminal."));
        }
    }
}