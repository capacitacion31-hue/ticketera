package com.example.ticketero.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@DisplayName("HealthController - Integration Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/health")
    class GetHealth {

        @Test
        @DisplayName("debe retornar status UP con información del servicio")
        void getHealth_debeRetornarStatusUp() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ticketero-api"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("debe retornar Content-Type application/json")
        void getHealth_debeRetornarContentTypeJson() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("debe retornar timestamp válido")
        void getHealth_debeRetornarTimestampValido() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isString());
        }

        @Test
        @DisplayName("debe ser accesible sin autenticación")
        void getHealth_debeSerAccesibleSinAuth() throws Exception {
            // When & Then - No authentication headers needed
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("debe retornar estructura completa del health check")
        void getHealth_debeRetornarEstructuraCompleta() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.service").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("ticketero-api"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
        }
    }
}