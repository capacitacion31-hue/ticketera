package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.AdvisorStatusRequest;
import com.example.ticketero.model.dto.response.AdvisorResponse;
import com.example.ticketero.model.dto.response.AdvisorStatsResponse;
import com.example.ticketero.model.dto.response.AdvisorStatusChangeResponse;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisorService {

    private final AdvisorRepository advisorRepository;
    private final AuditService auditService;

    public List<AdvisorResponse> getAllAdvisors() {
        return advisorRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AdvisorResponse> getAvailableAdvisors() {
        return advisorRepository.findByStatus(AdvisorStatus.AVAILABLE)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdvisorStatusChangeResponse changeStatus(Long advisorId, AdvisorStatusRequest request) {
        Advisor advisor = advisorRepository.findById(advisorId)
            .orElseThrow(() -> new IllegalArgumentException("Advisor not found: " + advisorId));

        AdvisorStatus previousStatus = advisor.getStatus();
        advisor.setStatus(request.status());
        
        // Reset workload si cambia a AVAILABLE
        if (request.status() == AdvisorStatus.AVAILABLE) {
            advisor.setWorkloadMinutes(0);
            advisor.setAssignedTicketsCount(0);
        }

        Advisor saved = advisorRepository.save(advisor);
        
        // Auditoría
        auditService.logAdvisorStatusChanged(saved, previousStatus, request.reason());
        
        log.info("Advisor {} status changed from {} to {}", 
            advisor.getName(), previousStatus, request.status());

        return new AdvisorStatusChangeResponse(
            saved.getId(),
            saved.getName(),
            saved.getStatus(),
            previousStatus,
            LocalDateTime.now(),
            "system", // En producción obtener del contexto de seguridad
            request.reason()
        );
    }

    public AdvisorStatsResponse getAdvisorStats(Long advisorId) {
        Advisor advisor = advisorRepository.findById(advisorId)
            .orElseThrow(() -> new IllegalArgumentException("Advisor not found: " + advisorId));

        // Placeholder - implementar cálculos reales de estadísticas
        AdvisorStatsResponse.AdvisorPerformance performance = 
            new AdvisorStatsResponse.AdvisorPerformance(
                advisor.getTotalTicketsServedToday(),
                advisor.getAverageServiceTimeMinutes().doubleValue(),
                15.0, // Tiempo estimado promedio
                93.0, // Accuracy
                "ABOVE_AVERAGE"
            );

        List<AdvisorStatsResponse.TicketDetail> ticketDetails = List.of(
            new AdvisorStatsResponse.TicketDetail(
                "C01", "CAJA", 5, 4, "-20%", "FASTER"
            )
        );

        return new AdvisorStatsResponse(
            advisor.getId(),
            advisor.getName(),
            LocalDate.now(),
            performance,
            ticketDetails
        );
    }

    private AdvisorResponse toResponse(Advisor advisor) {
        return new AdvisorResponse(
            advisor.getId(),
            advisor.getName(),
            advisor.getEmail(),
            advisor.getStatus(),
            advisor.getModuleNumber(),
            advisor.getAssignedTicketsCount(),
            advisor.getWorkloadMinutes(),
            advisor.getAverageServiceTimeMinutes(),
            advisor.getTotalTicketsServedToday(),
            advisor.getQueueTypes(),
            advisor.getLastAssignedAt(),
            LocalDateTime.now() // statusSince - placeholder
        );
    }
}