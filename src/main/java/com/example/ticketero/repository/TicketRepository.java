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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Query derivadas básicas
    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);
    
    Optional<Ticket> findByNumero(String numero);
    
    List<Ticket> findByNationalIdAndStatusIn(String nationalId, List<TicketStatus> statuses);
    
    List<Ticket> findByQueueTypeAndStatus(QueueType queueType, TicketStatus status);
    
    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
    
    boolean existsByNationalIdAndStatusIn(String nationalId, List<TicketStatus> statuses);
    
    long countByQueueTypeAndStatus(QueueType queueType, TicketStatus status);

    // Queries JPQL para lógica compleja
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.queueType = :queueType 
        AND t.status = 'EN_ESPERA' 
        ORDER BY t.createdAt ASC
        """)
    List<Ticket> findWaitingTicketsByQueue(@Param("queueType") QueueType queueType);

    @Query("""
        SELECT COUNT(t) FROM Ticket t 
        WHERE t.queueType = :queueType 
        AND t.status = 'EN_ESPERA' 
        AND t.createdAt < :createdAt
        """)
    long countTicketsAheadInQueue(
        @Param("queueType") QueueType queueType, 
        @Param("createdAt") LocalDateTime createdAt
    );

    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.createdAt < :timeLimit 
        AND t.status = 'EN_ESPERA'
        ORDER BY t.queueType DESC, t.createdAt ASC
        """)
    List<Ticket> findCriticalTickets(@Param("timeLimit") LocalDateTime timeLimit);
}