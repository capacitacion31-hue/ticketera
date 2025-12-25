package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AdvisorRepository Tests")
class AdvisorRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Nested
    @DisplayName("Query Derivadas")
    class QueryDerivadas {

        @Test
        @DisplayName("Debe encontrar advisors por estado")
        void debeEncontrarAdvisorsPorEstado() {
            // Given
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 0);
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.BUSY, 15);
            Advisor advisor3 = createAdvisor("Ana García", 3, AdvisorStatus.AVAILABLE, 5);
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);
            entityManager.persistAndFlush(advisor3);

            // When
            List<Advisor> found = advisorRepository.findByStatus(AdvisorStatus.AVAILABLE);

            // Then
            assertThat(found).hasSize(2);
            assertThat(found).extracting(Advisor::getName)
                .containsExactlyInAnyOrder("María López", "Ana García");
        }

        @Test
        @DisplayName("Debe encontrar advisor por número de módulo")
        void debeEncontrarAdvisorPorNumeroDeModulo() {
            // Given
            Advisor advisor = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 0);
            entityManager.persistAndFlush(advisor);

            // When
            Optional<Advisor> found = advisorRepository.findByModuleNumber(1);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("María López");
        }

        @Test
        @DisplayName("Debe encontrar advisors por estado ordenados por carga de trabajo")
        void debeEncontrarAdvisorsPorEstadoOrdenadosPorCargaDeTrabajo() {
            // Given
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 15);
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.AVAILABLE, 5);
            Advisor advisor3 = createAdvisor("Ana García", 3, AdvisorStatus.AVAILABLE, 10);
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);
            entityManager.persistAndFlush(advisor3);

            // When
            List<Advisor> found = advisorRepository.findByStatusOrderByWorkloadMinutesAsc(AdvisorStatus.AVAILABLE);

            // Then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getName()).isEqualTo("Juan Pérez");
            assertThat(found.get(1).getName()).isEqualTo("Ana García");
            assertThat(found.get(2).getName()).isEqualTo("María López");
        }

        @Test
        @DisplayName("Debe contar advisors por estado")
        void debeContarAdvisorsPorEstado() {
            // Given
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 0);
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.BUSY, 15);
            Advisor advisor3 = createAdvisor("Ana García", 3, AdvisorStatus.AVAILABLE, 5);
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);
            entityManager.persistAndFlush(advisor3);

            // When
            long count = advisorRepository.countByStatus(AdvisorStatus.AVAILABLE);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Queries JPQL para Asignación")
    class QueriesJPQLParaAsignacion {

        @Test
        @DisplayName("Debe encontrar advisors disponibles para cola")
        void debeEncontrarAdvisorsDisponiblesParaCola() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 10);
            advisor1.setLastAssignedAt(now.minusMinutes(30));
            
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.AVAILABLE, 5);
            advisor2.setLastAssignedAt(now.minusMinutes(15));
            
            Advisor advisor3 = createAdvisor("Ana García", 3, AdvisorStatus.BUSY, 20);
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);
            entityManager.persistAndFlush(advisor3);

            // When
            List<Advisor> found = advisorRepository.findAvailableAdvisorsForQueue();

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.get(0).getName()).isEqualTo("Juan Pérez"); // Menor carga
        }

        @Test
        @DisplayName("Debe encontrar advisors disponibles ordenados por carga")
        void debeEncontrarAdvisorsDisponiblesOrdenadosPorCarga() {
            // Given
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 20);
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.AVAILABLE, 5);
            Advisor advisor3 = createAdvisor("Ana García", 3, AdvisorStatus.AVAILABLE, 15);
            Advisor advisor4 = createAdvisor("Carlos Ruiz", 4, AdvisorStatus.OFFLINE, 0);
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);
            entityManager.persistAndFlush(advisor3);
            entityManager.persistAndFlush(advisor4);

            // When
            List<Advisor> found = advisorRepository.findAvailableAdvisorsOrderByWorkload();

            // Then
            assertThat(found).hasSize(3);
            assertThat(found.get(0).getName()).isEqualTo("Juan Pérez");
            assertThat(found.get(1).getName()).isEqualTo("Ana García");
            assertThat(found.get(2).getName()).isEqualTo("María López");
        }

        @Test
        @DisplayName("Debe manejar advisors sin última asignación")
        void debeManejarAdvisorsSinUltimaAsignacion() {
            // Given
            Advisor advisor1 = createAdvisor("María López", 1, AdvisorStatus.AVAILABLE, 10);
            advisor1.setLastAssignedAt(null); // Nunca asignado
            
            Advisor advisor2 = createAdvisor("Juan Pérez", 2, AdvisorStatus.AVAILABLE, 10);
            advisor2.setLastAssignedAt(LocalDateTime.now().minusMinutes(15));
            
            entityManager.persistAndFlush(advisor1);
            entityManager.persistAndFlush(advisor2);

            // When
            List<Advisor> found = advisorRepository.findAvailableAdvisorsForQueue();

            // Then
            assertThat(found).hasSize(2);
            // Los que nunca fueron asignados deben aparecer primero (NULLS FIRST)
            assertThat(found.get(0).getName()).isEqualTo("María López");
        }
    }

    private Advisor createAdvisor(String name, Integer moduleNumber, AdvisorStatus status, int workloadMinutes) {
        return Advisor.builder()
            .name(name)
            .email(name.toLowerCase().replace(" ", ".") + "@banco.com")
            .moduleNumber(moduleNumber)
            .status(status)
            .workloadMinutes(workloadMinutes)
            .queueTypes(List.of(QueueType.CAJA))
            .averageServiceTimeMinutes(BigDecimal.valueOf(5))
            .totalTicketsServedToday(0)
            .assignedTicketsCount(0)
            .createdAt(LocalDateTime.now())
            .build();
    }
}