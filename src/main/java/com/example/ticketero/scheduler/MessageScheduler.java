package com.example.ticketero.scheduler;

import com.example.ticketero.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageScheduler {

    private final MessageService messageService;

    @Scheduled(fixedDelay = 5000) // Cada 5 segundos
    public void processPendingMessages() {
        try {
            messageService.processPendingMessages();
        } catch (Exception e) {
            log.error("Error processing pending messages", e);
        }
    }

    @Scheduled(fixedDelay = 30000) // Cada 30 segundos
    public void processRetryMessages() {
        try {
            messageService.processRetryMessages();
        } catch (Exception e) {
            log.error("Error processing retry messages", e);
        }
    }
}