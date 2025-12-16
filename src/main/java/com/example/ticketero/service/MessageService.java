package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.EstadoEnvio;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private final MensajeRepository mensajeRepository;
    private final AuditService auditService;

    @Transactional
    public void scheduleTicketCreatedMessage(Ticket ticket) {
        if (ticket.getTelefono() == null) {
            log.debug("No phone number for ticket {}, skipping message", ticket.getNumero());
            return;
        }

        Mensaje mensaje = Mensaje.builder()
            .ticket(ticket)
            .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
            .estadoEnvio(EstadoEnvio.PENDIENTE)
            .fechaProgramada(LocalDateTime.now())
            .intentos(0)
            .build();

        mensajeRepository.save(mensaje);
        log.info("Scheduled TICKET_CREADO message for ticket {}", ticket.getNumero());
    }

    @Transactional
    public void scheduleProximoTurnoMessage(Ticket ticket) {
        if (ticket.getTelefono() == null || ticket.getPositionInQueue() > 3) {
            return;
        }

        Mensaje mensaje = Mensaje.builder()
            .ticket(ticket)
            .plantilla(MessageTemplate.TOTEM_PROXIMO_TURNO)
            .estadoEnvio(EstadoEnvio.PENDIENTE)
            .fechaProgramada(LocalDateTime.now())
            .intentos(0)
            .build();

        mensajeRepository.save(mensaje);
        log.info("Scheduled PROXIMO_TURNO message for ticket {}", ticket.getNumero());
    }

    @Transactional
    public void scheduleEsTuTurnoMessage(Ticket ticket) {
        if (ticket.getTelefono() == null) {
            return;
        }

        Mensaje mensaje = Mensaje.builder()
            .ticket(ticket)
            .plantilla(MessageTemplate.TOTEM_ES_TU_TURNO)
            .estadoEnvio(EstadoEnvio.PENDIENTE)
            .fechaProgramada(LocalDateTime.now())
            .intentos(0)
            .build();

        mensajeRepository.save(mensaje);
        log.info("Scheduled ES_TU_TURNO message for ticket {}", ticket.getNumero());
    }

    @Scheduled(fixedDelay = 5000) // Cada 5 segundos
    @Transactional
    public void processPendingMessages() {
        List<Mensaje> pendingMessages = mensajeRepository.findPendingMessages(LocalDateTime.now());
        
        for (Mensaje mensaje : pendingMessages) {
            sendMessage(mensaje);
        }
    }

    @Scheduled(fixedDelay = 30000) // Cada 30 segundos
    @Transactional
    public void processRetryMessages() {
        List<Mensaje> retryMessages = mensajeRepository.findRetryableMessages(LocalDateTime.now());
        
        for (Mensaje mensaje : retryMessages) {
            sendMessage(mensaje);
        }
    }

    @Async
    @Transactional
    public void sendMessage(Mensaje mensaje) {
        try {
            mensaje.setIntentos(mensaje.getIntentos() + 1);
            
            // Simular envío a Telegram API
            String messageText = buildMessageText(mensaje);
            String telegramMessageId = sendToTelegramAPI(mensaje.getTicket().getTelefono(), messageText);
            
            // Éxito
            mensaje.setEstadoEnvio(EstadoEnvio.ENVIADO);
            mensaje.setFechaEnvio(LocalDateTime.now());
            mensaje.setTelegramMessageId(telegramMessageId);
            
            log.info("Message sent successfully: {} for ticket {}", 
                mensaje.getPlantilla(), mensaje.getTicket().getNumero());
                
        } catch (Exception e) {
            log.error("Failed to send message for ticket {}: {}", 
                mensaje.getTicket().getNumero(), e.getMessage());
            
            if (mensaje.getIntentos() >= 4) {
                mensaje.setEstadoEnvio(EstadoEnvio.FALLIDO);
                log.error("Message failed permanently after 4 attempts: {}", mensaje.getId());
            } else {
                // Programar reintento con backoff exponencial
                int delayMinutes = calculateBackoffDelay(mensaje.getIntentos());
                mensaje.setFechaProgramada(LocalDateTime.now().plusMinutes(delayMinutes));
                log.info("Scheduled retry #{} in {} minutes for message {}", 
                    mensaje.getIntentos() + 1, delayMinutes, mensaje.getId());
            }
        }
        
        mensajeRepository.save(mensaje);
    }

    private String buildMessageText(Mensaje mensaje) {
        Ticket ticket = mensaje.getTicket();
        
        return switch (mensaje.getPlantilla()) {
            case TOTEM_TICKET_CREADO -> String.format("""
                ✅ <b>Ticket Creado</b>
                Tu número de turno: <b>%s</b>
                Posición en cola: <b>#%d</b>
                Tiempo estimado: <b>%d minutos</b>
                Te notificaremos cuando estés próximo.
                """, ticket.getNumero(), ticket.getPositionInQueue(), ticket.getEstimatedWaitMinutes());
                
            case TOTEM_PROXIMO_TURNO -> String.format("""
                ⏰ <b>¡Pronto será tu turno!</b>
                Turno: <b>%s</b>
                Faltan aproximadamente 3 turnos.
                Por favor, acércate a la sucursal.
                """, ticket.getNumero());
                
            case TOTEM_ES_TU_TURNO -> String.format("""
                🔔 <b>¡ES TU TURNO %s!</b>
                Dirígete al módulo: <b>%d</b>
                Asesor: <b>%s</b>
                """, ticket.getNumero(), 
                ticket.getAssignedModuleNumber(),
                ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : "N/A");
        };
    }

    private String sendToTelegramAPI(String phoneNumber, String messageText) {
        // Simulación de envío a Telegram API
        // En producción: HTTP call a https://api.telegram.org/bot{token}/sendMessage
        log.debug("Sending to {}: {}", phoneNumber, messageText);
        return "msg_" + System.currentTimeMillis();
    }

    private int calculateBackoffDelay(int attemptNumber) {
        // RN-008: Backoff exponencial (30s, 60s, 120s)
        return switch (attemptNumber) {
            case 1 -> 0;   // Inmediato
            case 2 -> 1;   // 30 segundos (convertido a minutos para simplificar)
            case 3 -> 1;   // 60 segundos
            case 4 -> 2;   // 120 segundos
            default -> 5;
        };
    }
}