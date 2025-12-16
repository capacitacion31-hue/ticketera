package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.AdvisorStatusRequest;
import com.example.ticketero.model.dto.response.*;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.service.AdvisorService;
import com.example.ticketero.service.AuditService;
import com.example.ticketero.service.DashboardService;
import com.example.ticketero.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final DashboardService dashboardService;
    private final QueueService queueService;
    private final AdvisorService advisorService;
    private final AuditService auditService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<PerformanceSummaryResponse> getPerformanceSummary() {
        PerformanceSummaryResponse response = dashboardService.getPerformanceSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queues")
    public ResponseEntity<List<QueueSummaryResponse>> getAllQueues() {
        List<QueueSummaryResponse> response = queueService.getAllQueuesSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queues/{type}")
    public ResponseEntity<QueueSummaryResponse> getQueueSummary(@PathVariable QueueType type) {
        QueueSummaryResponse response = queueService.getQueueSummary(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/queues/{type}/stats")
    public ResponseEntity<QueueStatsResponse> getQueueStats(@PathVariable QueueType type) {
        QueueStatsResponse response = queueService.getQueueStats(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/advisors")
    public ResponseEntity<List<AdvisorResponse>> getAllAdvisors() {
        List<AdvisorResponse> response = advisorService.getAllAdvisors();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/advisors/{id}/stats")
    public ResponseEntity<AdvisorStatsResponse> getAdvisorStats(@PathVariable Long id) {
        AdvisorStatsResponse response = advisorService.getAdvisorStats(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/advisors/{id}/status")
    public ResponseEntity<AdvisorStatusChangeResponse> changeAdvisorStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdvisorStatusRequest request
    ) {
        log.info("Changing advisor {} status to {}", id, request.status());
        AdvisorStatusChangeResponse response = advisorService.changeStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit/ticket/{ticketId}")
    public ResponseEntity<AuditEventResponse> getTicketAudit(@PathVariable String ticketId) {
        AuditEventResponse response = auditService.getAuditTrail("TICKET", ticketId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit/advisor/{advisorId}")
    public ResponseEntity<AuditEventResponse> getAdvisorAudit(@PathVariable String advisorId) {
        AuditEventResponse response = auditService.getAuditTrail("ADVISOR", advisorId);
        return ResponseEntity.ok(response);
    }
}