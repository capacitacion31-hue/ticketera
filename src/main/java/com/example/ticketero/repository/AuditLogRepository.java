package com.example.ticketero.repository;

import com.example.ticketero.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Query derivadas
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    
    List<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType);
    
    List<AuditLog> findByActorOrderByTimestampDesc(String actor);

    // Queries con paginación
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime start, 
        LocalDateTime end, 
        Pageable pageable
    );

    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.entityType = :entityType 
        AND a.entityId = :entityId 
        ORDER BY a.timestamp DESC
        """)
    List<AuditLog> findAuditTrailForEntity(
        @Param("entityType") String entityType, 
        @Param("entityId") String entityId
    );
}