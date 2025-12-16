package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {

    // Query derivadas
    List<Advisor> findByStatus(AdvisorStatus status);
    
    Optional<Advisor> findByModuleNumber(Integer moduleNumber);
    
    List<Advisor> findByStatusOrderByWorkloadMinutesAsc(AdvisorStatus status);
    
    long countByStatus(AdvisorStatus status);

    // Query para asignación inteligente
    @Query("""
        SELECT a FROM Advisor a 
        WHERE a.status = 'AVAILABLE' 
        ORDER BY a.workloadMinutes ASC, a.lastAssignedAt ASC NULLS FIRST
        """)
    List<Advisor> findAvailableAdvisorsForQueue();

    @Query("""
        SELECT a FROM Advisor a 
        WHERE a.status = 'AVAILABLE'
        ORDER BY a.workloadMinutes ASC
        """)
    List<Advisor> findAvailableAdvisorsOrderByWorkload();
}