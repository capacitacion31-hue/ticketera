package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueueStatsRepository extends JpaRepository<Ticket, Long> {

    // Estadísticas por cola
    @Query(value = """
        SELECT COUNT(*) FROM tickets t 
        WHERE t.queue_type = CAST(:queueType AS VARCHAR) 
        AND t.status = CAST(:status AS VARCHAR) 
        AND DATE(t.created_at) = CURRENT_DATE
        """, nativeQuery = true)
    long countTodayByQueueAndStatus(
        @Param("queueType") QueueType queueType, 
        @Param("status") TicketStatus status
    );

    @Query(value = """
        SELECT AVG(t.actual_service_time_minutes) FROM tickets t 
        WHERE t.queue_type = CAST(:queueType AS VARCHAR) 
        AND t.status = 'COMPLETADO' 
        AND DATE(t.completed_at) = CURRENT_DATE
        """, nativeQuery = true)
    Double getAverageServiceTimeToday(@Param("queueType") QueueType queueType);

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (t.assigned_at - t.created_at))/60) FROM tickets t 
        WHERE t.queue_type = CAST(:queueType AS VARCHAR) 
        AND t.assigned_at IS NOT NULL 
        AND DATE(t.created_at) = CURRENT_DATE
        """, nativeQuery = true)
    Double getAverageWaitTimeToday(@Param("queueType") QueueType queueType);

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
        ORDER BY t.queueType DESC, t.createdAt ASC
        """)
    List<Ticket> findCriticalTicketsByTimeLimit(
        @Param("cajaLimit") LocalDateTime cajaLimit,
        @Param("personalLimit") LocalDateTime personalLimit,
        @Param("empresasLimit") LocalDateTime empresasLimit,
        @Param("gerenciaLimit") LocalDateTime gerenciaLimit
    );
}