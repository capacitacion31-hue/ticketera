package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueueStatsRepository extends JpaRepository<Ticket, Long> {

    // Estadísticas por cola - usando queries derivadas simples
    long countByQueueTypeAndStatusAndCreatedAtBetween(
        QueueType queueType, 
        TicketStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    // Queries JPQL simples para estadísticas
    @Query("""
        SELECT AVG(CAST(t.actualServiceTimeMinutes AS double)) FROM Ticket t 
        WHERE t.queueType = :queueType 
        AND t.status = 'COMPLETADO' 
        AND t.completedAt BETWEEN :startDate AND :endDate
        """)
    Double getAverageServiceTimeBetween(
        @Param("queueType") QueueType queueType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.queueType = :queueType 
        AND t.assignedAt IS NOT NULL 
        AND t.createdAt BETWEEN :startDate AND :endDate
        """)
    List<Ticket> findAssignedTicketsBetween(
        @Param("queueType") QueueType queueType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // Tickets críticos por tiempo límite
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.status = 'EN_ESPERA' 
        AND (
            (t.queueType = 'CAJA' AND t.createdAt < :cajaLimit) OR
            (t.queueType = 'PERSONAL_BANKER' AND t.createdAt < :personalLimit) OR
            (t.queueType = 'EMPRESAS' AND t.createdAt < :empresasLimit) OR
            (t.queueType = 'GERENCIA' AND t.createdAt < :gerenciaLimit)
        )
        ORDER BY 
            CASE t.queueType 
                WHEN 'GERENCIA' THEN 4
                WHEN 'EMPRESAS' THEN 3
                WHEN 'PERSONAL_BANKER' THEN 2
                WHEN 'CAJA' THEN 1
            END DESC, 
            t.createdAt ASC
        """)
    List<Ticket> findCriticalTicketsByTimeLimit(
        @Param("cajaLimit") LocalDateTime cajaLimit,
        @Param("personalLimit") LocalDateTime personalLimit,
        @Param("empresasLimit") LocalDateTime empresasLimit,
        @Param("gerenciaLimit") LocalDateTime gerenciaLimit
    );

    // Métodos de conveniencia que usan la fecha actual
    default long countTodayByQueueAndStatus(QueueType queueType, TicketStatus status, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return countByQueueTypeAndStatusAndCreatedAtBetween(queueType, status, startOfDay, endOfDay);
    }

    default Double getAverageServiceTimeToday(QueueType queueType, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return getAverageServiceTimeBetween(queueType, startOfDay, endOfDay);
    }

    default Double getAverageWaitTimeToday(QueueType queueType, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<Ticket> tickets = findAssignedTicketsBetween(queueType, startOfDay, endOfDay);
        if (tickets.isEmpty()) {
            return null;
        }
        
        double totalWaitMinutes = tickets.stream()
            .mapToLong(ticket -> {
                LocalDateTime created = ticket.getCreatedAt();
                LocalDateTime assigned = ticket.getAssignedAt();
                return java.time.Duration.between(created, assigned).toMinutes();
            })
            .average()
            .orElse(0.0);
            
        return totalWaitMinutes;
    }

    default long countTodayByQueueAndStatus(QueueType queueType, TicketStatus status) {
        return countTodayByQueueAndStatus(queueType, status, LocalDate.now());
    }

    default Double getAverageServiceTimeToday(QueueType queueType) {
        return getAverageServiceTimeToday(queueType, LocalDate.now());
    }

    default Double getAverageWaitTimeToday(QueueType queueType) {
        return getAverageWaitTimeToday(queueType, LocalDate.now());
    }
}