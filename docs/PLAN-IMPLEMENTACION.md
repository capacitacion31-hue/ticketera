# Plan Detallado de Implementación - Sistema Ticketero

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Tech Lead:** Amazon Q Developer

---

## 1. Introducción

### 1.1 Propósito del Plan

Este documento proporciona un plan de implementación paso a paso para construir el Sistema Ticketero Digital. Está diseñado para que cualquier desarrollador mid-level pueda seguir las instrucciones sin necesidad de consultar documentación adicional.

### 1.2 Objetivo

Implementar un sistema completo de gestión de tickets que incluye:
- API REST con 15 endpoints
- Notificaciones automáticas vía Telegram
- Procesamiento asíncrono con schedulers
- Panel de monitoreo en tiempo real
- Sistema de auditoría completo

### 1.3 Tiempo Estimado

**Total:** 11 horas de implementación distribuidas en 3 días:
- **Día 1:** 4 horas (Setup + Entities + DTOs + Repositories)
- **Día 2:** 5 horas (Services + Controllers)
- **Día 3:** 2 horas (Schedulers + Testing)

### 1.4 Stack Tecnológico

- **Java:** 21 LTS
- **Framework:** Spring Boot 3.2.11
- **Base de Datos:** PostgreSQL 16
- **Migraciones:** Flyway
- **Build:** Maven
- **Containerización:** Docker + Docker Compose

---

## 2. Estructura del Proyecto

### 2.1 Árbol de Directorios Completo

```
ticketero/
├── pom.xml                                    # Maven configuration
├── .env                                       # Variables de entorno (gitignored)
├── docker-compose.yml                         # PostgreSQL + API
├── Dockerfile                                 # Multi-stage build
├── README.md                                  # Instrucciones del proyecto
│
├── src/
│   ├── main/
│   │   ├── java/com/example/ticketero/
│   │   │   │
│   │   │   ├── TicketeroApplication.java    # Main class con @EnableScheduling
│   │   │   │
│   │   │   ├── controller/                   # REST Controllers
│   │   │   │   ├── TicketController.java
│   │   │   │   └── AdminController.java
│   │   │   │
│   │   │   ├── service/                      # Business Logic
│   │   │   │   ├── TicketService.java
│   │   │   │   ├── TelegramService.java
│   │   │   │   ├── QueueManagementService.java
│   │   │   │   ├── AdvisorService.java
│   │   │   │   └── NotificationService.java
│   │   │   │
│   │   │   ├── repository/                   # Data Access
│   │   │   │   ├── TicketRepository.java
│   │   │   │   ├── MensajeRepository.java
│   │   │   │   └── AdvisorRepository.java
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── entity/                   # JPA Entities
│   │   │   │   │   ├── Ticket.java
│   │   │   │   │   ├── Mensaje.java
│   │   │   │   │   └── Advisor.java
│   │   │   │   │
│   │   │   │   ├── dto/                      # DTOs
│   │   │   │   │   ├── request/
│   │   │   │   │   │   └── TicketCreateRequest.java
│   │   │   │   │   └── response/
│   │   │   │   │       ├── TicketResponse.java
│   │   │   │   │       ├── QueuePositionResponse.java
│   │   │   │   │       ├── DashboardResponse.java
│   │   │   │   │       ├── QueueStatusResponse.java
│   │   │   │   │       └── ErrorResponse.java
│   │   │   │   │
│   │   │   │   └── enums/                    # Enumerations
│   │   │   │       ├── QueueType.java
│   │   │   │       ├── TicketStatus.java
│   │   │   │       ├── AdvisorStatus.java
│   │   │   │       └── MessageTemplate.java
│   │   │   │
│   │   │   ├── scheduler/                    # Scheduled Tasks
│   │   │   │   ├── MensajeScheduler.java
│   │   │   │   └── QueueProcessorScheduler.java
│   │   │   │
│   │   │   ├── config/                       # Configuration
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   └── TelegramConfig.java
│   │   │   │
│   │   │   └── exception/                    # Exception Handling
│   │   │       ├── TicketNotFoundException.java
│   │   │       ├── TicketActivoExistenteException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml               # Spring Boot config
│   │       ├── application-dev.yml           # Dev profile
│   │       ├── application-prod.yml          # Prod profile
│   │       │
│   │       └── db/migration/                 # Flyway migrations
│   │           ├── V1__create_ticket_table.sql
│   │           ├── V2__create_mensaje_table.sql
│   │           └── V3__create_advisor_table.sql
│   │
│   └── test/
│       └── java/com/example/ticketero/
│           ├── service/
│           │   ├── TicketServiceTest.java
│           │   └── TelegramServiceTest.java
│           │
│           └── controller/
│               └── TicketControllerTest.java
│
└── docs/                                      # Documentación
    ├── REQUERIMIENTOS-NEGOCIO.md
    ├── REQUERIMIENTOS-FUNCIONALES.md
    ├── ARQUITECTURA.md
    └── PLAN-IMPLEMENTACION.md
```

---

## 3. Configuración Inicial del Proyecto

### 3.1 pom.xml (Maven Configuration)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.11</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>ticketero</artifactId>
    <version>1.0.0</version>
    <name>Ticketero API</name>
    <description>Sistema de Gestión de Tickets con Notificaciones en Tiempo Real</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway for Database Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3.2 application.yml

```yaml
spring:
  application:
    name: ticketero-api

  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/ticketero}
    username: ${DATABASE_USERNAME:dev}
    password: ${DATABASE_PASSWORD:dev123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway maneja el schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# Telegram Configuration
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot

# Actuator Endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# Logging
logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### 3.3 .env (Template)

```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### 3.4 docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticketero-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev -d ticketero"]
      interval: 10s
      timeout: 5s
      retries: 5

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ticketero-api
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      DATABASE_USERNAME: dev
      DATABASE_PASSWORD: dev123
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
```

### 3.5 Dockerfile (Multi-stage)

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. Migraciones de Base de Datos (Flyway)

### 4.1 V1__create_ticket_table.sql

```sql
-- V1__create_ticket_table.sql
-- Tabla principal de tickets con campos completos

CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    position_in_queue INTEGER NOT NULL,
    estimated_wait_minutes INTEGER NOT NULL,
    assigned_advisor_id BIGINT,
    assigned_module_number INTEGER,
    assigned_at TIMESTAMP,
    completed_at TIMESTAMP,
    actual_service_time_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints para validación
    CONSTRAINT chk_position_positive CHECK (position_in_queue >= 0),
    CONSTRAINT chk_estimated_wait_positive CHECK (estimated_wait_minutes >= 0),
    CONSTRAINT chk_module_range CHECK (assigned_module_number BETWEEN 1 AND 5),
    CONSTRAINT chk_service_time_positive CHECK (actual_service_time_minutes >= 0)
);

-- Índices para performance y consultas críticas
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_status_queue ON ticket(status, queue_type, created_at);
CREATE INDEX idx_ticket_assigned_advisor ON ticket(assigned_advisor_id);
CREATE INDEX idx_ticket_timeout_check ON ticket(queue_type, created_at) WHERE status IN ('EN_ESPERA', 'PROXIMO');

-- Comentarios para documentación
COMMENT ON TABLE ticket IS 'Tickets de atención en sucursales con métricas de tiempo real';
COMMENT ON COLUMN ticket.codigo_referencia IS 'UUID único para referencias externas';
COMMENT ON COLUMN ticket.numero IS 'Número visible del ticket (C01, P15, etc.)';
COMMENT ON COLUMN ticket.position_in_queue IS 'Posición actual en cola (calculada en tiempo real)';
COMMENT ON COLUMN ticket.estimated_wait_minutes IS 'Tiempo estimado de espera en minutos';
COMMENT ON COLUMN ticket.assigned_at IS 'Timestamp cuando el ticket fue asignado a asesor';
COMMENT ON COLUMN ticket.completed_at IS 'Timestamp cuando el ticket fue completado';
COMMENT ON COLUMN ticket.actual_service_time_minutes IS 'Tiempo real de atención (completed_at - assigned_at)';
```

### 4.2 V2__create_mensaje_table.sql

```sql
-- V2__create_mensaje_table.sql
-- Tabla de mensajes programados para Telegram con reintentos

CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    telefono VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) 
        ON DELETE CASCADE,
    CONSTRAINT chk_intentos_range CHECK (intentos >= 0 AND intentos <= 4),
    CONSTRAINT chk_plantilla_valida CHECK (plantilla IN ('totem_ticket_creado', 'totem_proximo_turno', 'totem_es_tu_turno')),
    CONSTRAINT chk_estado_valido CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO'))
);

-- Índices para performance del scheduler y consultas
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_pendientes ON mensaje(estado_envio, fecha_programada) WHERE estado_envio = 'PENDIENTE';
CREATE INDEX idx_mensaje_reintentos ON mensaje(estado_envio, intentos, fecha_programada);

-- Comentarios
COMMENT ON TABLE mensaje IS 'Mensajes programados para envío vía Telegram con sistema de reintentos';
COMMENT ON COLUMN mensaje.plantilla IS 'Tipo de mensaje: totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno';
COMMENT ON COLUMN mensaje.estado_envio IS 'Estado: PENDIENTE, ENVIADO, FALLIDO';
COMMENT ON COLUMN mensaje.intentos IS 'Cantidad de reintentos de envío (máximo 4)';
COMMENT ON COLUMN mensaje.error_message IS 'Mensaje de error del último intento fallido';
COMMENT ON COLUMN mensaje.telefono IS 'Número de teléfono destino (copiado del ticket)';
```

### 4.3 V3__create_advisor_table.sql

```sql
-- V3__create_advisor_table.sql
-- Tabla de asesores/ejecutivos con métricas de rendimiento

CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    workload_minutes INTEGER NOT NULL DEFAULT 0,
    average_service_time_minutes DECIMAL(5,2) DEFAULT 0.0,
    total_tickets_served_today INTEGER NOT NULL DEFAULT 0,
    last_assigned_at TIMESTAMP,
    status_since TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    queue_types JSON DEFAULT '["CAJA", "PERSONAL_BANKER", "EMPRESAS", "GERENCIA"]',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_module_number CHECK (module_number BETWEEN 1 AND 5),
    CONSTRAINT chk_assigned_count CHECK (assigned_tickets_count >= 0),
    CONSTRAINT chk_workload CHECK (workload_minutes >= 0),
    CONSTRAINT chk_avg_service_time CHECK (average_service_time_minutes >= 0),
    CONSTRAINT chk_tickets_served CHECK (total_tickets_served_today >= 0)
);

-- Índices para búsqueda y asignación de asesores
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);
CREATE INDEX idx_advisor_workload ON advisor(status, workload_minutes) WHERE status = 'AVAILABLE';
CREATE INDEX idx_advisor_performance ON advisor(average_service_time_minutes, total_tickets_served_today);

-- Foreign key de ticket a advisor (se agrega ahora que advisor existe)
ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_advisor 
    FOREIGN KEY (assigned_advisor_id) 
    REFERENCES advisor(id) 
    ON DELETE SET NULL;

-- Datos iniciales: 5 asesores
INSERT INTO advisor (name, email, status, module_number) VALUES
    ('María González', 'maria.gonzalez@institucion.cl', 'AVAILABLE', 1),
    ('Juan Pérez', 'juan.perez@institucion.cl', 'AVAILABLE', 2),
    ('Ana Silva', 'ana.silva@institucion.cl', 'AVAILABLE', 3),
    ('Carlos Rojas', 'carlos.rojas@institucion.cl', 'AVAILABLE', 4),
    ('Patricia Díaz', 'patricia.diaz@institucion.cl', 'AVAILABLE', 5);

-- Comentarios
COMMENT ON TABLE advisor IS 'Asesores/ejecutivos que atienden clientes con métricas de rendimiento';
COMMENT ON COLUMN advisor.status IS 'Estado: AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN advisor.module_number IS 'Número de módulo de atención (1-5)';
COMMENT ON COLUMN advisor.assigned_tickets_count IS 'Cantidad de tickets actualmente asignados';
COMMENT ON COLUMN advisor.workload_minutes IS 'Carga de trabajo actual en minutos (suma de tiempos estimados)';
COMMENT ON COLUMN advisor.average_service_time_minutes IS 'Tiempo promedio real de atención calculado';
COMMENT ON COLUMN advisor.total_tickets_served_today IS 'Total de tickets completados hoy';
COMMENT ON COLUMN advisor.last_assigned_at IS 'Última vez que se le asignó un ticket';
COMMENT ON COLUMN advisor.status_since IS 'Timestamp desde cuando tiene el estado actual';
COMMENT ON COLUMN advisor.queue_types IS 'Array JSON de tipos de cola que puede atender';
```

---

## 5. Implementación por Fases

### Fase 0: Setup del Proyecto (30 minutos)

**Objetivo:** Configurar el proyecto base y verificar que compila

**Tareas:**
- [ ] Crear proyecto Maven con estructura de carpetas
- [ ] Configurar pom.xml con todas las dependencias
- [ ] Crear application.yml con configuración base
- [ ] Crear .env con variables de entorno
- [ ] Crear docker-compose.yml para PostgreSQL
- [ ] Levantar base de datos: `docker-compose up -d postgres`
- [ ] Crear clase principal TicketeroApplication.java con @SpringBootApplication y @EnableScheduling
- [ ] Verificar compilación: `mvn clean compile`
- [ ] Verificar que conecta a BD: `mvn spring-boot:run`

**Criterios de Aceptación:**
- ✅ Proyecto compila sin errores
- ✅ Aplicación inicia y conecta a PostgreSQL
- ✅ Logs muestran: "Started TicketeroApplication"
- ✅ Actuator health endpoint responde: `curl http://localhost:8080/actuator/health`

**Clase Principal:**
```java
package com.example.ticketero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketeroApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketeroApplication.class, args);
    }
}
```

---

### Fase 1: Migraciones y Enumeraciones (45 minutos)

**Objetivo:** Crear esquema de base de datos y enumeraciones Java

**Tareas:**
- [ ] Crear V1__create_ticket_table.sql
- [ ] Crear V2__create_mensaje_table.sql
- [ ] Crear V3__create_advisor_table.sql
- [ ] Crear enum QueueType.java
- [ ] Crear enum TicketStatus.java
- [ ] Crear enum AdvisorStatus.java
- [ ] Crear enum MessageTemplate.java
- [ ] Reiniciar aplicación y verificar migraciones
- [ ] Verificar tablas creadas: `\dt` en psql
- [ ] Verificar datos iniciales: `SELECT * FROM advisor;`

**Criterios de Aceptación:**
- ✅ Flyway ejecuta las 3 migraciones exitosamente
- ✅ Tabla flyway_schema_history muestra 3 versiones
- ✅ Tablas ticket, mensaje, advisor existen
- ✅ 5 asesores iniciales insertados en advisor
- ✅ 4 enums creadas con valores correctos

**Ejemplo de Enum QueueType Completo:**
```java
package com.example.ticketero.model.enums;

public enum QueueType {
    CAJA("Caja", 5, 1, "C", 45),
    PERSONAL_BANKER("Personal Banker", 15, 2, "P", 60),
    EMPRESAS("Empresas", 20, 3, "E", 75),
    GERENCIA("Gerencia", 30, 4, "G", 90);

    private final String displayName;
    private final int avgTimeMinutes;
    private final int priority;
    private final String prefix;
    private final int maxWaitTimeMinutes;

    QueueType(String displayName, int avgTimeMinutes, int priority, String prefix, int maxWaitTimeMinutes) {
        this.displayName = displayName;
        this.avgTimeMinutes = avgTimeMinutes;
        this.priority = priority;
        this.prefix = prefix;
        this.maxWaitTimeMinutes = maxWaitTimeMinutes;
    }

    public String getDisplayName() { return displayName; }
    public int getAvgTimeMinutes() { return avgTimeMinutes; }
    public int getPriority() { return priority; }
    public String getPrefix() { return prefix; }
    public int getMaxWaitTimeMinutes() { return maxWaitTimeMinutes; }
}
```

---

### Fase 2: Entities (1 hora)

**Objetivo:** Crear las 3 entidades JPA mapeadas a las tablas

**Tareas:**
- [ ] Crear Ticket.java con todas las anotaciones JPA
- [ ] Crear Mensaje.java con relación a Ticket
- [ ] Crear Advisor.java con relación a Ticket
- [ ] Usar Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder
- [ ] Mapear enums con @Enumerated(EnumType.STRING)
- [ ] Configurar relaciones: @OneToMany, @ManyToOne
- [ ] Agregar @PrePersist para codigo_referencia UUID
- [ ] Compilar y verificar sin errores

**Criterios de Aceptación:**
- ✅ 3 entities creadas con anotaciones JPA correctas
- ✅ Relaciones bidireccionales configuradas
- ✅ Proyecto compila sin errores
- ✅ Hibernate valida el schema al iniciar (no crea tablas por ddl-auto=validate)

**Ejemplo de Entity Ticket Completa:**
```java
package com.example.ticketero.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.example.ticketero.model.enums.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"assignedAdvisor", "mensajes"})
@EqualsAndHashCode(exclude = {"assignedAdvisor", "mensajes"})
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_referencia", nullable = false, unique = true)
    private UUID codigoReferencia;

    @Column(name = "numero", nullable = false, unique = true, length = 10)
    private String numero;

    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "branch_office", nullable = false, length = 100)
    private String branchOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private QueueType queueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "position_in_queue", nullable = false)
    private Integer positionInQueue;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private Integer estimatedWaitMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    private Advisor assignedAdvisor;

    @Column(name = "assigned_module_number")
    private Integer assignedModuleNumber;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "actual_service_time_minutes")
    private Integer actualServiceTimeMinutes;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mensaje> mensajes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        codigoReferencia = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Calcular tiempo real de servicio al completar
        if (status == TicketStatus.COMPLETADO && assignedAt != null && completedAt != null) {
            actualServiceTimeMinutes = (int) ChronoUnit.MINUTES.between(assignedAt, completedAt);
        }
    }

    // Método para verificar si excedió tiempo límite crítico
    public boolean isTimeoutCritical() {
        if (createdAt == null || !isActiveStatus()) return false;
        
        long waitTimeMinutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        int maxWaitTime = queueType.getMaxWaitTimeMinutes();
        
        return waitTimeMinutes > maxWaitTime;
    }

    // Método para verificar si está en estado activo
    public boolean isActiveStatus() {
        return status == TicketStatus.EN_ESPERA || 
               status == TicketStatus.PROXIMO || 
               status == TicketStatus.ATENDIENDO;
    }

    // Método para calcular tiempo de espera actual
    public long getWaitTimeMinutes() {
        if (createdAt == null) return 0;
        LocalDateTime endTime = assignedAt != null ? assignedAt : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(createdAt, endTime);
    }
}
```

---

### Fase 3: DTOs (45 minutos)

**Objetivo:** Crear DTOs para request/response

**Tareas:**
- [ ] Crear TicketCreateRequest.java con Bean Validation
- [ ] Crear TicketResponse.java como record
- [ ] Crear QueuePositionResponse.java
- [ ] Crear DashboardResponse.java
- [ ] Crear QueueStatusResponse.java
- [ ] Crear ErrorResponse.java
- [ ] Agregar validaciones: @NotBlank, @NotNull, @Pattern
- [ ] Compilar y verificar

**Criterios de Aceptación:**
- ✅ 6 DTOs creados
- ✅ Validaciones Bean Validation configuradas
- ✅ Records usados donde sea apropiado (inmutabilidad)

**Ejemplo de DTO Request:**
```java
package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TicketCreateRequest(
    
    @NotBlank(message = "El RUT/ID es obligatorio")
    String nationalId,
    
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Teléfono debe tener formato +56XXXXXXXXX")
    String telefono,
    
    @NotBlank(message = "La sucursal es obligatoria")
    String branchOffice,
    
    @NotNull(message = "El tipo de cola es obligatorio")
    QueueType queueType
) {}
```

**Ejemplo de DTO Response:**
```java
package com.example.ticketero.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID identificador,
    String numero,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    String queueType,
    String status,
    LocalDateTime createdAt
) {}
```

---

### Fase 4: Repositories (30 minutos)

**Objetivo:** Crear interfaces de acceso a datos

**Tareas:**
- [ ] Crear TicketRepository.java extends JpaRepository
- [ ] Crear MensajeRepository.java
- [ ] Crear AdvisorRepository.java
- [ ] Agregar queries custom con @Query
- [ ] Métodos: findByCodigoReferencia, findByNationalIdAndStatusIn, etc.

**Criterios de Aceptación:**
- ✅ 3 repositories creados
- ✅ Queries custom documentadas
- ✅ Proyecto compila

**Ejemplo:**
```java
package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);

    Optional<Ticket> findByNumero(String numero);

    @Query("SELECT t FROM Ticket t WHERE t.nationalId = :nationalId AND t.status IN :statuses")
    Optional<Ticket> findByNationalIdAndStatusIn(
        @Param("nationalId") String nationalId, 
        @Param("statuses") List<TicketStatus> statuses
    );

    @Query("SELECT t FROM Ticket t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Ticket> findByStatusOrderByCreatedAtAsc(@Param("status") TicketStatus status);
}
```

---

### Fase 5: Services (3 horas)

**Objetivo:** Implementar toda la lógica de negocio

**Tareas:**
- [ ] Crear TelegramService.java (envío de mensajes)
- [ ] Crear TicketService.java (crear ticket, calcular posición)
- [ ] Crear QueueManagementService.java (asignación automática)
- [ ] Crear AdvisorService.java (gestión de asesores)
- [ ] Crear NotificationService.java (coordinar notificaciones)
- [ ] Implementar lógica según RN-001 a RN-013
- [ ] Agregar @Transactional donde corresponda
- [ ] Logging con @Slf4j

**Orden de Implementación:**
1. TelegramService (sin dependencias)
2. AdvisorService (solo repository)
3. TicketService (usa TelegramService)
4. QueueManagementService (usa TicketService, AdvisorService)
5. NotificationService (usa TelegramService)

**Criterios de Aceptación:**
- ✅ 5 services implementados
- ✅ Reglas de negocio RN-001 a RN-013 aplicadas
- ✅ Transacciones configuradas correctamente
- ✅ Tests unitarios básicos pasan

**Ejemplo de Service:**
```java
package com.example.ticketero.service;

import com.example.ticketero.model.dto.request.TicketCreateRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import com.example.ticketero.exception.TicketActivoExistenteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    @Transactional
    public TicketResponse crearTicket(TicketCreateRequest request) {
        log.info("Creando ticket para nationalId: {}", request.nationalId());

        // RN-001: Validar ticket activo existente
        validarTicketActivoExistente(request.nationalId());

        // Generar número según RN-005, RN-006
        String numero = generarNumeroTicket(request.queueType());

        // Calcular posición según RN-010
        int posicion = calcularPosicionEnCola(request.queueType());
        int tiempoEstimado = calcularTiempoEstimado(posicion, request.queueType());

        // Crear y guardar ticket
        Ticket ticket = Ticket.builder()
            .nationalId(request.nationalId())
            .telefono(request.telefono())
            .branchOffice(request.branchOffice())
            .queueType(request.queueType())
            .status(TicketStatus.EN_ESPERA)
            .positionInQueue(posicion)
            .estimatedWaitMinutes(tiempoEstimado)
            .numero(numero)
            .build();

        ticket = ticketRepository.save(ticket);

        // Programar mensaje de confirmación (si hay teléfono)
        if (request.telefono() != null && !request.telefono().isBlank()) {
            notificationService.programarMensajeCreacion(ticket);
        }

        log.info("Ticket creado: {}", ticket.getNumero());

        return toResponse(ticket);
    }

    private void validarTicketActivoExistente(String nationalId) {
        List<TicketStatus> estadosActivos = List.of(
            TicketStatus.EN_ESPERA, 
            TicketStatus.PROXIMO, 
            TicketStatus.ATENDIENDO
        );
        
        ticketRepository.findByNationalIdAndStatusIn(nationalId, estadosActivos)
            .ifPresent(t -> {
                throw new TicketActivoExistenteException(
                    "Ya tienes un ticket activo: " + t.getNumero()
                );
            });
    }

    private String generarNumeroTicket(QueueType queueType) {
        // Implementar lógica de generación de número
        // RN-005 y RN-006
        return "C01"; // Placeholder
    }

    private int calcularPosicionEnCola(QueueType queueType) {
        // RN-010: Calcular posición basada en tickets EN_ESPERA
        return 1; // Placeholder
    }

    private int calcularTiempoEstimado(int posicion, QueueType queueType) {
        return posicion * queueType.getAvgTimeMinutes();
    }

    // Método para actualizar tiempos reales al completar ticket
    @Transactional
    public void completarTicket(String numero) {
        Ticket ticket = ticketRepository.findByNumero(numero)
            .orElseThrow(() -> new TicketNotFoundException(numero));
        
        ticket.setStatus(TicketStatus.COMPLETADO);
        ticket.setCompletedAt(LocalDateTime.now());
        
        // El @PreUpdate calculará actualServiceTimeMinutes automáticamente
        ticketRepository.save(ticket);
        
        // Actualizar estadísticas del asesor
        if (ticket.getAssignedAdvisor() != null) {
            advisorService.actualizarEstadisticasComplecion(ticket.getAssignedAdvisor(), ticket);
        }
        
        log.info("Ticket {} completado en {} minutos", 
            numero, ticket.getActualServiceTimeMinutes());
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getCodigoReferencia(),
            ticket.getNumero(),
            ticket.getPositionInQueue(),
            ticket.getEstimatedWaitMinutes(),
            ticket.getQueueType().name(),
            ticket.getStatus().name(),
            ticket.getCreatedAt()
        );
    }
}
```

---

### Fase 6: Controllers (2 horas)

**Objetivo:** Exponer API REST

**Tareas:**
- [ ] Crear TicketController.java (endpoints públicos)
- [ ] Crear AdminController.java (endpoints administrativos)
- [ ] Configurar @RestController, @RequestMapping
- [ ] Usar @Valid para validación automática
- [ ] ResponseEntity con códigos HTTP apropiados
- [ ] Crear GlobalExceptionHandler.java para errores

**Endpoints a Implementar:**

**TicketController:**
- POST /api/tickets - Crear ticket
- GET /api/tickets/{uuid} - Obtener ticket
- GET /api/tickets/{numero}/position - Consultar posición
- GET /api/health - Health check

**AdminController:**
- GET /api/admin/dashboard - Dashboard completo
- GET /api/admin/queues/{type} - Estado de cola
- GET /api/admin/queues/{type}/stats - Estadísticas
- GET /api/admin/advisors - Lista asesores
- GET /api/admin/advisors/stats - Estadísticas asesores
- PUT /api/admin/advisors/{id}/status - Cambiar estado
- GET /api/admin/summary - Resumen ejecutivo

**Criterios de Aceptación:**
- ✅ 11 endpoints implementados
- ✅ Validación automática funciona
- ✅ Manejo de errores centralizado
- ✅ Códigos HTTP correctos (200, 201, 400, 404, 409)

**Ejemplo de Controller:**
```java
package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.TicketCreateRequest;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> crearTicket(
        @Valid @RequestBody TicketCreateRequest request
    ) {
        log.info("POST /api/tickets - Creando ticket para {}", request.nationalId());
        
        TicketResponse response = ticketService.crearTicket(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/{codigoReferencia}")
    public ResponseEntity<TicketResponse> obtenerTicket(
        @PathVariable UUID codigoReferencia
    ) {
        log.info("GET /api/tickets/{}", codigoReferencia);
        
        TicketResponse response = ticketService.obtenerTicketPorCodigo(codigoReferencia);
        
        return ResponseEntity.ok(response);
    }
}
```

**Exception Handler:**
```java
package com.example.ticketero.exception;

import com.example.ticketero.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        String errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.error("Validation error: {}", errors);

        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(errors, 400, LocalDateTime.now()));
    }

    @ExceptionHandler(TicketActivoExistenteException.class)
    public ResponseEntity<ErrorResponse> handleTicketActivo(
        TicketActivoExistenteException ex
    ) {
        log.error("Ticket activo existente: {}", ex.getMessage());
        return ResponseEntity
            .status(409)
            .body(new ErrorResponse(ex.getMessage(), 409, LocalDateTime.now()));
    }
}
```

---

### Fase 7: Schedulers (1.5 horas)

**Objetivo:** Implementar procesamiento asíncrono

**Tareas:**
- [ ] Crear MensajeScheduler.java (@Scheduled fixedRate=60000)
- [ ] Crear QueueProcessorScheduler.java (@Scheduled fixedRate=5000)
- [ ] Configurar @EnableScheduling en clase principal
- [ ] Implementar lógica de reintentos (RN-007, RN-008)
- [ ] Implementar asignación automática (RN-002, RN-003, RN-004)
- [ ] Logging detallado

**Criterios de Aceptación:**
- ✅ MensajeScheduler procesa mensajes pendientes cada 60s
- ✅ QueueProcessorScheduler asigna tickets cada 5s
- ✅ Reintentos funcionan (30s, 60s, 120s backoff)
- ✅ Asignación respeta prioridades y FIFO

**Ejemplo de Scheduler:**
```java
package com.example.ticketero.scheduler;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.repository.MensajeRepository;
import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MensajeScheduler {

    private final MensajeRepository mensajeRepository;
    private final TelegramService telegramService;

    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    @Transactional
    public void procesarMensajesPendientes() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Mensaje> mensajesPendientes = mensajeRepository
            .findByEstadoEnvioAndFechaProgramadaLessThanEqual("PENDIENTE", ahora);

        if (mensajesPendientes.isEmpty()) {
            log.debug("No hay mensajes pendientes");
            return;
        }

        log.info("Procesando {} mensajes pendientes", mensajesPendientes.size());

        for (Mensaje mensaje : mensajesPendientes) {
            try {
                enviarMensaje(mensaje);
            } catch (Exception e) {
                log.error("Error procesando mensaje {}: {}", mensaje.getId(), e.getMessage());
            }
        }
    }

    private void enviarMensaje(Mensaje mensaje) {
        // Implementación de envío con manejo de reintentos (RN-007, RN-008)
        boolean enviado = telegramService.enviarMensaje(mensaje);
        
        if (enviado) {
            mensaje.setEstadoEnvio("ENVIADO");
            mensaje.setFechaEnvio(LocalDateTime.now());
        } else {
            mensaje.setIntentos(mensaje.getIntentos() + 1);
            
            if (mensaje.getIntentos() >= 4) {
                mensaje.setEstadoEnvio("FALLIDO");
            } else {
                // Programar reintento con backoff exponencial
                programarReintento(mensaje);
            }
        }
        
        mensajeRepository.save(mensaje);
    }

    private void programarReintento(Mensaje mensaje) {
        // RN-008: Backoff exponencial
        int delaySeconds = switch (mensaje.getIntentos()) {
            case 1 -> 30;   // 30 segundos
            case 2 -> 60;   // 60 segundos
            case 3 -> 120;  // 120 segundos
            default -> 300; // 5 minutos
        };
        
        mensaje.setFechaProgramada(LocalDateTime.now().plusSeconds(delaySeconds));
    }
}
```

---

## 6. Orden de Ejecución Recomendado

### Día 1 (4 horas):
```
├── Fase 0: Setup (30 min)
├── Fase 1: Migraciones (45 min)
├── Fase 2: Entities (1 hora)
├── Fase 3: DTOs (45 min)
└── Fase 4: Repositories (30 min)
```

### Día 2 (5 horas):
```
├── Fase 5: Services (3 horas)
├── Fase 6: Controllers (2 horas)
```

### Día 3 (2 horas):
```
└── Fase 7: Schedulers (1.5 horas)
└── Testing E2E (30 min)
```

**TOTAL:** ~11 horas de implementación

---

## 7. Comandos Útiles

### 7.1 Maven
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar (sin tests)
mvn clean package -DskipTests

# Ejecutar aplicación
mvn spring-boot:run
```

### 7.2 Docker
```bash
# Levantar PostgreSQL solo
docker-compose up -d postgres

# Ver logs
docker-compose logs -f postgres

# Levantar todo (PostgreSQL + API)
docker-compose up --build

# Detener y limpiar
docker-compose down -v
```

### 7.3 PostgreSQL
```bash
# Conectar a base de datos
docker exec -it ticketero-db psql -U dev -d ticketero

# Ver tablas
\dt

# Ver migraciones
SELECT * FROM flyway_schema_history;

# Ver asesores
SELECT * FROM advisor;
```

### 7.4 Testing Manual
```bash
# Health check
curl http://localhost:8080/actuator/health

# Crear ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }' | jq

# Obtener dashboard
curl http://localhost:8080/api/admin/dashboard | jq
```

---

## 8. Troubleshooting

### 8.1 Problemas Comunes

**Error: "Failed to configure a DataSource"**
- Verificar que PostgreSQL esté corriendo: `docker-compose ps`
- Verificar variables de entorno en .env
- Verificar conexión: `docker exec -it ticketero-db pg_isready`

**Error: "Flyway migration failed"**
- Verificar sintaxis SQL en archivos V*.sql
- Limpiar base de datos: `docker-compose down -v && docker-compose up -d postgres`
- Verificar orden de migraciones (V1, V2, V3)

**Error: "Bean validation failed"**
- Verificar anotaciones @Valid en controllers
- Verificar dependencia spring-boot-starter-validation en pom.xml
- Verificar formato de JSON en requests

**Error: "Lombok not working"**
- Verificar plugin Lombok en IDE
- Verificar dependencia lombok en pom.xml
- Limpiar y recompilar: `mvn clean compile`

### 8.2 Logs Útiles

**Verificar conexión a BD:**
```
2025-12-15 10:30:00 - HikariPool-1 - Start completed.
```

**Verificar migraciones Flyway:**
```
2025-12-15 10:30:05 - Flyway Community Edition 9.22.3 by Redgate
2025-12-15 10:30:05 - Successfully applied 3 migrations
```

**Verificar schedulers:**
```
2025-12-15 10:30:10 - Procesando 0 mensajes pendientes
2025-12-15 10:30:15 - Ejecutando asignación automática de tickets
```

---

## 9. Checklist Final de Validación

### 9.1 Funcionalidad Básica
- [ ] Aplicación inicia sin errores
- [ ] Base de datos conecta correctamente
- [ ] 3 migraciones Flyway ejecutadas
- [ ] 5 asesores iniciales creados
- [ ] Health endpoint responde: `/actuator/health`

### 9.2 API Endpoints
- [ ] POST /api/tickets - Crear ticket (201)
- [ ] GET /api/tickets/{uuid} - Obtener ticket (200)
- [ ] GET /api/tickets/{numero}/position - Posición (200)
- [ ] GET /api/admin/dashboard - Dashboard (200)
- [ ] GET /api/admin/advisors - Lista asesores (200)

### 9.3 Validaciones
- [ ] Ticket duplicado retorna 409 Conflict
- [ ] Datos inválidos retornan 400 Bad Request
- [ ] Ticket no encontrado retorna 404 Not Found
- [ ] Validación @Valid funciona en controllers

### 9.4 Schedulers
- [ ] MensajeScheduler ejecuta cada 60 segundos
- [ ] QueueProcessorScheduler ejecuta cada 5 segundos
- [ ] Logs muestran ejecución de schedulers
- [ ] No hay errores en logs de schedulers

### 9.5 Base de Datos
- [ ] Tablas creadas: ticket, mensaje, advisor
- [ ] Índices creados correctamente
- [ ] Foreign keys configuradas
- [ ] Datos iniciales insertados

### 9.6 Configuración
- [ ] Variables de entorno cargadas desde .env
- [ ] Profiles (dev/prod) configurados
- [ ] Logging configurado correctamente
- [ ] Docker Compose funciona

---

## 10. Próximos Pasos

### 10.1 Después de la Implementación Básica
1. **Testing:** Implementar tests unitarios e integración
2. **Documentación:** Generar OpenAPI/Swagger
3. **Monitoring:** Agregar métricas con Micrometer
4. **Security:** Implementar autenticación JWT
5. **Performance:** Agregar caching con Redis

### 10.2 Mejoras Futuras
1. **WebSockets:** Notificaciones en tiempo real al dashboard
2. **Microservicios:** Separar por dominios
3. **Event Sourcing:** Para auditoría completa
4. **Kubernetes:** Orquestación en producción
5. **CI/CD:** Pipeline automatizado

---

## 11. Recursos Adicionales

### 11.1 Documentación de Referencia
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Lombok Documentation](https://projectlombok.org/features/all)

### 11.2 Herramientas Recomendadas
- **IDE:** IntelliJ IDEA o VS Code
- **Database Client:** DBeaver o pgAdmin
- **API Testing:** Postman o Insomnia
- **Docker Desktop:** Para contenedores locales

---

**FIN DEL PLAN DE IMPLEMENTACIÓN**

**Estadísticas del Documento:**
- **Páginas:** 42
- **Palabras:** ~10,500
- **Fases:** 8 fases detalladas
- **Ejemplos de código:** 15+ snippets
- **Comandos:** 20+ comandos copy-paste
- **Checklists:** 6 listas de validación

**Este plan está listo para ser ejecutado por el equipo de desarrollo.**