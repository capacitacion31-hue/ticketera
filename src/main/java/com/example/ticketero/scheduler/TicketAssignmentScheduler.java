package com.example.ticketero.scheduler;

import com.example.ticketero.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketAssignmentScheduler {

    private final AssignmentService assignmentService;

    @Scheduled(fixedDelay = 3000) // Cada 3 segundos
    public void processTicketAssignments() {
        try {
            assignmentService.processTicketAssignments();
        } catch (Exception e) {
            log.error("Error processing ticket assignments", e);
        }
    }
}