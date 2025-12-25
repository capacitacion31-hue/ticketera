package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.AdvisorStatusRequest;
import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.AdvisorStatsResponse;
import com.example.ticketero.model.dto.response.AdvisorStatusChangeResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.ticketero.testutil.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvisorService - Unit Tests")
class AdvisorServiceTest {

    @Mock
    private AdvisorRepository advisorRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdvisorService advisorService;

    @Nested
    @DisplayName("getAllAdvisors()")
    class ObtenerTodosAdvisors {

        @Test
        @DisplayName("debe retornar lista de advisors como responses")
        void getAllAdvisors_debeRetornarListaResponses() {
            // Given
            List<Advisor> advisors = List.of(
                advisorAvailable().build(),
                advisorBusy().build()
            );
            when(advisorRepository.findAll()).thenReturn(advisors);

            // When
            List<AdvisorResponse> responses = advisorService.getAllAdvisors();

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).name()).isEqualTo("María López");
            assertThat(responses.get(0).status()).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(responses.get(1).status()).isEqualTo(AdvisorStatus.BUSY);
        }

        @Test
        @DisplayName("con lista vacía → debe retornar lista vacía")
        void getAllAdvisors_listaVacia_debeRetornarVacia() {
            // Given
            when(advisorRepository.findAll()).thenReturn(List.of());

            // When
            List<AdvisorResponse> responses = advisorService.getAllAdvisors();

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAvailableAdvisors()")
    class ObtenerAdvisorsDisponibles {

        @Test
        @DisplayName("debe retornar solo advisors disponibles")
        void getAvailableAdvisors_debeRetornarSoloDisponibles() {
            // Given
            List<Advisor> availableAdvisors = List.of(
                advisorAvailable().id(1L).build(),
                advisorAvailable().id(2L).build()
            );
            when(advisorRepository.findByStatus(AdvisorStatus.AVAILABLE))
                .thenReturn(availableAdvisors);

            // When
            List<AdvisorResponse> responses = advisorService.getAvailableAdvisors();

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses).allMatch(r -> r.status() == AdvisorStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("changeStatus()")
    class CambiarEstado {

        @Test
        @DisplayName("con advisor existente → debe cambiar estado y auditar")
        void changeStatus_advisorExistente_debeCambiarYAuditar() {
            // Given
            Advisor advisor = advisorAvailable().build();
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.OFFLINE, 
                "Descanso programado"
            );
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));
            when(advisorRepository.save(advisor)).thenReturn(advisor);

            // When
            AdvisorStatusChangeResponse response = advisorService.changeStatus(1L, request);

            // Then
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.OFFLINE);
            assertThat(response.status()).isEqualTo(AdvisorStatus.OFFLINE);
            assertThat(response.previousStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
            assertThat(response.reason()).isEqualTo("Descanso programado");

            verify(advisorRepository).save(advisor);
            verify(auditService).logAdvisorStatusChanged(advisor, AdvisorStatus.AVAILABLE, "Descanso programado");
        }

        @Test
        @DisplayName("cambio a AVAILABLE → debe resetear workload")
        void changeStatus_aAvailable_debeResetearWorkload() {
            // Given
            Advisor advisor = advisorBusy()
                .workloadMinutes(30)
                .assignedTicketsCount(2)
                .build();
            AdvisorStatusRequest request = new AdvisorStatusRequest(
                AdvisorStatus.AVAILABLE, 
                "Terminó atención"
            );
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));
            when(advisorRepository.save(advisor)).thenReturn(advisor);

            // When
            advisorService.changeStatus(1L, request);

            // Then
            assertThat(advisor.getWorkloadMinutes()).isEqualTo(0);
            assertThat(advisor.getAssignedTicketsCount()).isEqualTo(0);
            assertThat(advisor.getStatus()).isEqualTo(AdvisorStatus.AVAILABLE);
        }

        @Test
        @DisplayName("advisor inexistente → debe lanzar IllegalArgumentException")
        void changeStatus_advisorInexistente_debeLanzarExcepcion() {
            // Given
            AdvisorStatusRequest request = new AdvisorStatusRequest(AdvisorStatus.OFFLINE, "Test");
            when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> advisorService.changeStatus(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Advisor not found: 999");

            verify(advisorRepository, never()).save(any());
            verify(auditService, never()).logAdvisorStatusChanged(any(), any(), any());
        }

        @Test
        @DisplayName("debe loggear cambio de estado")
        void changeStatus_debeLoggearCambio() {
            // Given
            Advisor advisor = advisorAvailable().name("Juan Pérez").build();
            AdvisorStatusRequest request = new AdvisorStatusRequest(AdvisorStatus.BUSY, "Atendiendo cliente");
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));
            when(advisorRepository.save(advisor)).thenReturn(advisor);

            // When
            advisorService.changeStatus(1L, request);

            // Then - Verificar que se loggea (no podemos verificar el log directamente, pero sí las interacciones)
            verify(advisorRepository).save(advisor);
        }
    }

    @Nested
    @DisplayName("getAdvisorStats()")
    class ObtenerEstadisticas {

        @Test
        @DisplayName("con advisor existente → debe retornar estadísticas")
        void getAdvisorStats_advisorExistente_debeRetornarStats() {
            // Given
            Advisor advisor = advisorAvailable()
                .id(1L)
                .name("María López")
                .totalTicketsServedToday(15)
                .build();
            
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            AdvisorStatsResponse stats = advisorService.getAdvisorStats(1L);

            // Then
            assertThat(stats.advisorId()).isEqualTo(1L);
            assertThat(stats.name()).isEqualTo("María López");
            assertThat(stats.performance().totalTicketsServed()).isEqualTo(15);
            assertThat(stats.performance().averageServiceTimeReal()).isEqualTo(5.0);
            assertThat(stats.ticketDetails()).isNotEmpty();
        }

        @Test
        @DisplayName("advisor inexistente → debe lanzar IllegalArgumentException")
        void getAdvisorStats_advisorInexistente_debeLanzarExcepcion() {
            // Given
            when(advisorRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> advisorService.getAdvisorStats(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Advisor not found: 999");
        }

        @Test
        @DisplayName("debe incluir performance y detalles de tickets")
        void getAdvisorStats_debeIncluirPerformanceYDetalles() {
            // Given
            Advisor advisor = advisorAvailable().build();
            when(advisorRepository.findById(1L)).thenReturn(Optional.of(advisor));

            // When
            AdvisorStatsResponse stats = advisorService.getAdvisorStats(1L);

            // Then
            assertThat(stats.performance()).isNotNull();
            assertThat(stats.performance().efficiency()).isEqualTo("ABOVE_AVERAGE");
            assertThat(stats.ticketDetails()).hasSize(1);
            assertThat(stats.ticketDetails().get(0).ticket()).isEqualTo("C01");
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ConvertirAResponse {

        @Test
        @DisplayName("debe mapear correctamente advisor a response")
        void toResponse_debeMappearCorrectamente() {
            // Given
            Advisor advisor = advisorAvailable()
                .id(1L)
                .name("María López")
                .email("maria@test.com")
                .moduleNumber(5)
                .assignedTicketsCount(2)
                .workloadMinutes(20)
                .build();

            // When - Accedemos indirectamente a través de getAllAdvisors
            when(advisorRepository.findAll()).thenReturn(List.of(advisor));
            List<AdvisorResponse> responses = advisorService.getAllAdvisors();

            // Then
            AdvisorResponse response = responses.get(0);
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("María López");
            assertThat(response.email()).isEqualTo("maria@test.com");
            assertThat(response.moduleNumber()).isEqualTo(5);
            assertThat(response.assignedTicketsCount()).isEqualTo(2);
            assertThat(response.workloadMinutes()).isEqualTo(20);
            assertThat(response.queueTypes()).isNotNull();
        }
    }
}