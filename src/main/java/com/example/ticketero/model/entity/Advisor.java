package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.QueueType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "advisor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvisorStatus status;

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    @Column(name = "assigned_tickets_count", nullable = false)
    @Builder.Default
    private Integer assignedTicketsCount = 0;

    @Column(name = "workload_minutes", nullable = false)
    @Builder.Default
    private Integer workloadMinutes = 0;

    @Column(name = "average_service_time_minutes", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageServiceTimeMinutes = BigDecimal.ZERO;

    @Column(name = "total_tickets_served_today", nullable = false)
    @Builder.Default
    private Integer totalTicketsServedToday = 0;

    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "queue_types", nullable = false)
    private List<QueueType> queueTypes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}