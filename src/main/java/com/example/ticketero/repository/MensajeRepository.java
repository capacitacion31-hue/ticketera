package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.enums.EstadoEnvio;
import com.example.ticketero.model.enums.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Query derivadas
    List<Mensaje> findByEstadoEnvio(EstadoEnvio estadoEnvio);
    
    List<Mensaje> findByTicketId(Long ticketId);
    
    List<Mensaje> findByPlantilla(MessageTemplate plantilla);

    // Queries para scheduler de mensajes
    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'PENDIENTE' 
        AND m.fechaProgramada <= :now 
        ORDER BY m.fechaProgramada ASC
        """)
    List<Mensaje> findPendingMessages(@Param("now") LocalDateTime now);

    @Query("""
        SELECT m FROM Mensaje m 
        WHERE m.estadoEnvio = 'FALLIDO' 
        AND m.intentos < 4 
        AND m.fechaProgramada <= :now
        ORDER BY m.fechaProgramada ASC
        """)
    List<Mensaje> findRetryableMessages(@Param("now") LocalDateTime now);
}