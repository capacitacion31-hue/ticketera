# Database Schema - Sistema Ticketero Digital

**Base de Datos:** PostgreSQL 16  
**Herramienta de Migración:** Flyway  
**Charset:** UTF-8  
**Timezone:** UTC

---

## 📋 Overview

El modelo de datos del Sistema Ticketero está diseñado para soportar:
- ✅ Gestión eficiente de tickets con estados y transiciones
- ✅ Asignación automática de asesores con balanceo de carga
- ✅ Sistema de mensajería programada con reintentos
- ✅ Auditoría completa de eventos del sistema
- ✅ Consultas de performance optimizadas con índices estratégicos

**Entidades principales:** 4 tablas core + índices de performance

---

## 🗄️ Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│     advisor     │       │     ticket      │
│─────────────────│       │─────────────────│
│ id (PK)         │◄─────┐│ id (PK)         │
│ name            │      ││ codigo_referencia│
│ email           │      ││ numero          │
│ status          │      ││ national_id     │
│ module_number   │      ││ telefono        │
│ assigned_tickets│      ││ branch_office   │
│ queue_types     │      ││ queue_type      │
│ ...             │      ││ status          │
└─────────────────┘      ││ position_in_queue│
                         ││ assigned_advisor_id (FK)
                         ││ assigned_module_number
                         ││ ...             │
                         │└─────────────────┘
                         │         │
                         │         │ 1:N
                         │         ▼
                         │┌─────────────────┐
                         ││     mensaje     │
                         ││─────────────────│
                         ││ id (PK)         │
                         │└─ ticket_id (FK) │
                         │ │ plantilla       │
                         │ │ estado_envio    │
                         │ │ fecha_programada│
                         │ │ intentos        │
                         │ │ ...             │
                         │ └─────────────────┘
                         │
                         │ 1:N
                         ▼
                ┌─────────────────┐
                │   audit_log     │
                │─────────────────│
                │ id (PK)         │
                │ entity_type     │
                │ entity_id       │
                │ event_type      │
                │ old_state       │
                │ new_state       │
                │ ...             │
                └─────────────────┘
```

---

## 📊 Tables

### 1. advisor
**Descripción:** Asesores que atienden clientes en módulos numerados del 1 al 5

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Identificador único del asesor |
| name | VARCHAR(100) | NOT NULL | Nombre completo del asesor |
| email | VARCHAR(100) | NOT NULL, UNIQUE | Email corporativo único |
| status | VARCHAR(20) | NOT NULL, CHECK | Estado: AVAILABLE, BUSY, OFFLINE |
| module_number | INTEGER | NOT NULL, CHECK (1-5) | Número del módulo asignado |
| assigned_tickets_count | INTEGER | NOT NULL, DEFAULT 0 | Contador de tickets asignados actualmente |
| workload_minutes | INTEGER | NOT NULL, DEFAULT 0 | Carga de trabajo ponderada en minutos |
| average_service_time_minutes | DECIMAL(5,2) | DEFAULT 0 | Tiempo promedio real de atención |
| total_tickets_served_today | INTEGER | NOT NULL, DEFAULT 0 | Total de tickets completados hoy |
| last_assigned_at | TIMESTAMP | NULL | Última vez que se le asignó un ticket |
| queue_types | JSON | NOT NULL | Tipos de cola que puede atender |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha de creación del registro |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Última actualización |

**Ejemplo de datos:**
```sql
INSERT INTO advisor (name, email, status, module_number, queue_types) VALUES
('María González', 'maria.gonzalez@banco.com', 'AVAILABLE', 3, '["PERSONAL_BANKER", "CAJA"]'),
('Carlos Rodríguez', 'carlos.rodriguez@banco.com', 'BUSY', 1, '["CAJA", "PERSONAL_BANKER"]'),
('Ana Martínez', 'ana.martinez@banco.com', 'AVAILABLE', 5, '["EMPRESAS", "GERENCIA"]');
```

**Reglas de negocio:**
- Un asesor solo puede estar en un módulo a la vez
- `assigned_tickets_count` se incrementa al asignar, decrementa al completar
- `queue_types` define qué tipos de cola puede atender cada asesor
- `workload_minutes` se calcula como: assigned_tickets_count × tiempo_promedio_cola

---

### 2. ticket
**Descripción:** Tickets de atención en sucursales con estados y asignaciones

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Identificador único interno |
| codigo_referencia | UUID | NOT NULL, UNIQUE | UUID para referencias externas |
| numero | VARCHAR(10) | NOT NULL, UNIQUE | Número visible (C01, P15, E03, G02) |
| national_id | VARCHAR(20) | NOT NULL | RUT/ID nacional del cliente |
| telefono | VARCHAR(20) | NULL | Número para notificaciones Telegram |
| branch_office | VARCHAR(100) | NOT NULL | Nombre de la sucursal |
| queue_type | VARCHAR(20) | NOT NULL, CHECK | CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA |
| status | VARCHAR(20) | NOT NULL, CHECK | Estado actual del ticket |
| position_in_queue | INTEGER | NOT NULL | Posición actual en cola |
| estimated_wait_minutes | INTEGER | NOT NULL | Tiempo estimado de espera |
| assigned_advisor_id | BIGINT | NULL, FK | Referencia al asesor asignado |
| assigned_module_number | INTEGER | NULL, CHECK (1-5) | Número del módulo asignado |
| assigned_at | TIMESTAMP | NULL | Momento de asignación a asesor |
| completed_at | TIMESTAMP | NULL | Momento de finalización |
| actual_service_time_minutes | INTEGER | NULL | Tiempo real de atención |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Última actualización |

**Estados válidos:**
```sql
CHECK (status IN ('EN_ESPERA', 'PROXIMO', 'ATENDIENDO', 'COMPLETADO', 'CANCELADO', 'NO_ATENDIDO'))
```

**Tipos de cola válidos:**
```sql
CHECK (queue_type IN ('CAJA', 'PERSONAL_BANKER', 'EMPRESAS', 'GERENCIA'))
```

**Ejemplo de datos:**
```sql
INSERT INTO ticket (codigo_referencia, numero, national_id, telefono, branch_office, queue_type, status, position_in_queue, estimated_wait_minutes) VALUES
('a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6', 'P01', '12345678-9', '+56912345678', 'Sucursal Centro', 'PERSONAL_BANKER', 'EN_ESPERA', 5, 75),
('b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7', 'C15', '98765432-1', '+56987654321', 'Sucursal Centro', 'CAJA', 'ATENDIENDO', 0, 0);
```

**Reglas de negocio:**
- `codigo_referencia` se genera automáticamente como UUID
- `numero` sigue formato: [Prefijo][01-99] donde prefijo = C/P/E/G
- Un cliente (`national_id`) solo puede tener 1 ticket activo a la vez
- `position_in_queue` se recalcula cada 5 segundos por el scheduler
- `actual_service_time_minutes` = (completed_at - assigned_at) en minutos

---

### 3. mensaje
**Descripción:** Mensajes programados para envío vía Telegram con sistema de reintentos

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Identificador único del mensaje |
| ticket_id | BIGINT | NOT NULL, FK | Referencia al ticket asociado |
| plantilla | VARCHAR(50) | NOT NULL | Plantilla del mensaje |
| estado_envio | VARCHAR(20) | NOT NULL, CHECK | PENDIENTE, ENVIADO, FALLIDO |
| fecha_programada | TIMESTAMP | NOT NULL | Cuándo debe enviarse |
| fecha_envio | TIMESTAMP | NULL | Cuándo se envió exitosamente |
| telegram_message_id | VARCHAR(50) | NULL | ID retornado por Telegram API |
| intentos | INTEGER | NOT NULL, DEFAULT 0 | Contador de intentos de envío |
| error_message | TEXT | NULL | Último mensaje de error |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha de creación |

**Estados válidos:**
```sql
CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO'))
```

**Plantillas válidas:**
```sql
CHECK (plantilla IN ('TOTEM_TICKET_CREADO', 'TOTEM_PROXIMO_TURNO', 'TOTEM_ES_TU_TURNO'))
```

**Ejemplo de datos:**
```sql
INSERT INTO mensaje (ticket_id, plantilla, estado_envio, fecha_programada) VALUES
(1, 'TOTEM_TICKET_CREADO', 'ENVIADO', '2024-12-15 10:30:00'),
(1, 'TOTEM_PROXIMO_TURNO', 'PENDIENTE', '2024-12-15 11:15:00'),
(1, 'TOTEM_ES_TU_TURNO', 'PENDIENTE', '2024-12-15 11:30:00');
```

**Reglas de negocio:**
- Cada ticket genera automáticamente 3 mensajes programados
- `intentos` se incrementa en cada reintento (máximo 3)
- Backoff exponencial: 30s, 60s, 120s entre reintentos
- `telegram_message_id` se almacena solo si el envío es exitoso

---

### 4. audit_log
**Descripción:** Registro de auditoría para todos los eventos críticos del sistema

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Identificador único del evento |
| entity_type | VARCHAR(50) | NOT NULL | Tipo de entidad (TICKET, ADVISOR, MENSAJE) |
| entity_id | VARCHAR(50) | NOT NULL | ID de la entidad afectada |
| event_type | VARCHAR(50) | NOT NULL | Tipo de evento |
| actor | VARCHAR(100) | NULL | Quién ejecutó la acción |
| old_state | JSON | NULL | Estado anterior (para cambios) |
| new_state | JSON | NULL | Estado nuevo (para cambios) |
| additional_data | JSON | NULL | Datos adicionales del evento |
| ip_address | VARCHAR(45) | NULL | IP desde donde se ejecutó |
| user_agent | VARCHAR(500) | NULL | User agent del cliente |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Timestamp del evento |

**Tipos de eventos:**
```sql
-- Eventos de tickets
TICKET_CREADO, TICKET_ASIGNADO, TICKET_COMPLETADO, TICKET_CANCELADO

-- Eventos de asesores  
ADVISOR_STATUS_CHANGED, ADVISOR_ASSIGNED, ADVISOR_FREED

-- Eventos de mensajes
MENSAJE_ENVIADO, MENSAJE_FALLIDO, MENSAJE_REINTENTADO

-- Eventos del sistema
SISTEMA_INICIADO, SCHEDULER_ERROR
```

**Ejemplo de datos:**
```sql
INSERT INTO audit_log (entity_type, entity_id, event_type, actor, new_state) VALUES
('TICKET', '1', 'TICKET_CREADO', 'SYSTEM', '{"numero": "P01", "status": "EN_ESPERA", "nationalId": "12345678-9"}'),
('ADVISOR', '1', 'ADVISOR_STATUS_CHANGED', 'supervisor@banco.com', '{"status": "BUSY", "assignedTicket": "P01"}');
```

---

## 🔍 Indexes

### Performance Indexes

**advisor table:**
```sql
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);
CREATE INDEX idx_advisor_workload ON advisor(workload_minutes);
CREATE INDEX idx_advisor_last_assigned ON advisor(last_assigned_at);
```

**ticket table:**
```sql
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_assigned_advisor ON ticket(assigned_advisor_id);
CREATE INDEX idx_ticket_position ON ticket(position_in_queue);
CREATE INDEX idx_ticket_queue_status ON ticket(queue_type, status);
```

**mensaje table:**
```sql
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_plantilla ON mensaje(plantilla);
CREATE INDEX idx_mensaje_intentos ON mensaje(intentos);
```

**audit_log table:**
```sql
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_created_at ON audit_log(created_at DESC);
CREATE INDEX idx_audit_actor ON audit_log(actor);
```

### Query Optimization

**Consultas más frecuentes optimizadas:**

1. **Buscar tickets activos por RUT:**
```sql
-- Optimizada con idx_ticket_national_id + idx_ticket_status
SELECT * FROM ticket 
WHERE national_id = ? AND status IN ('EN_ESPERA', 'PROXIMO', 'ATENDIENDO');
```

2. **Seleccionar asesor disponible con menor carga:**
```sql
-- Optimizada con idx_advisor_status + idx_advisor_workload
SELECT * FROM advisor 
WHERE status = 'AVAILABLE' 
ORDER BY workload_minutes ASC, last_assigned_at ASC NULLS FIRST 
LIMIT 1;
```

3. **Mensajes pendientes para scheduler:**
```sql
-- Optimizada con idx_mensaje_estado_fecha
SELECT * FROM mensaje 
WHERE estado_envio = 'PENDIENTE' AND fecha_programada <= NOW()
ORDER BY fecha_programada ASC;
```

4. **Tickets en cola por prioridad:**
```sql
-- Optimizada con idx_ticket_queue_status
SELECT * FROM ticket 
WHERE status = 'EN_ESPERA' 
ORDER BY 
  CASE queue_type 
    WHEN 'GERENCIA' THEN 4
    WHEN 'EMPRESAS' THEN 3  
    WHEN 'PERSONAL_BANKER' THEN 2
    WHEN 'CAJA' THEN 1
  END DESC,
  created_at ASC;
```

---

## 🗂️ Migrations

### Flyway Migration Files

**V1__create_advisor_table.sql**
```sql
-- Crear tabla advisor con constraints y comentarios
CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE')),
    module_number INTEGER NOT NULL CHECK (module_number BETWEEN 1 AND 5),
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    workload_minutes INTEGER NOT NULL DEFAULT 0,
    average_service_time_minutes DECIMAL(5,2) DEFAULT 0,
    total_tickets_served_today INTEGER NOT NULL DEFAULT 0,
    last_assigned_at TIMESTAMP,
    queue_types JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);
CREATE INDEX idx_advisor_workload ON advisor(workload_minutes);
CREATE INDEX idx_advisor_last_assigned ON advisor(last_assigned_at);

-- Datos iniciales
INSERT INTO advisor (name, email, status, module_number, queue_types) VALUES
('María González', 'maria.gonzalez@banco.com', 'AVAILABLE', 3, '["PERSONAL_BANKER", "CAJA"]'),
('Carlos Rodríguez', 'carlos.rodriguez@banco.com', 'AVAILABLE', 1, '["CAJA", "PERSONAL_BANKER"]'),
('Ana Martínez', 'ana.martinez@banco.com', 'AVAILABLE', 5, '["EMPRESAS", "GERENCIA"]'),
('Luis Pérez', 'luis.perez@banco.com', 'AVAILABLE', 2, '["CAJA", "PERSONAL_BANKER"]'),
('Carmen Silva', 'carmen.silva@banco.com', 'AVAILABLE', 4, '["EMPRESAS", "GERENCIA", "PERSONAL_BANKER"]');
```

**V2__create_ticket_table.sql**
```sql
-- Crear tabla ticket con todas las constraints
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    codigo_referencia UUID NOT NULL UNIQUE,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL CHECK (queue_type IN ('CAJA', 'PERSONAL_BANKER', 'EMPRESAS', 'GERENCIA')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('EN_ESPERA', 'PROXIMO', 'ATENDIENDO', 'COMPLETADO', 'CANCELADO', 'NO_ATENDIDO')),
    position_in_queue INTEGER NOT NULL,
    estimated_wait_minutes INTEGER NOT NULL,
    assigned_advisor_id BIGINT REFERENCES advisor(id),
    assigned_module_number INTEGER CHECK (assigned_module_number BETWEEN 1 AND 5),
    assigned_at TIMESTAMP,
    completed_at TIMESTAMP,
    actual_service_time_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices estratégicos
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_assigned_advisor ON ticket(assigned_advisor_id);
CREATE INDEX idx_ticket_position ON ticket(position_in_queue);
CREATE INDEX idx_ticket_queue_status ON ticket(queue_type, status);
```

**V3__create_mensaje_audit_tables.sql**
```sql
-- Tabla de mensajes programados
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    plantilla VARCHAR(50) NOT NULL CHECK (plantilla IN ('totem_ticket_creado', 'totem_proximo_turno', 'totem_es_tu_turno')),
    estado_envio VARCHAR(20) NOT NULL CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de auditoría
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor VARCHAR(100),
    old_state JSON,
    new_state JSON,
    additional_data JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para mensaje
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_plantilla ON mensaje(plantilla);

-- Índices para audit_log
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_created_at ON audit_log(created_at DESC);
```

### Migration Strategy

**Desarrollo:**
```bash
# Aplicar migraciones automáticamente al iniciar
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

**Producción:**
```bash
# Ejecutar migraciones manualmente antes del deploy
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db:5432/ticketero
```

**Rollback (si es necesario):**
```bash
# Flyway Community no soporta rollback automático
# Crear migration manual V4__rollback_v3.sql si es necesario
```

---

## 📊 Data Volume Estimates

### Proyecciones de Crecimiento

| Tabla | Registros/Día | Registros/Mes | Registros/Año | Tamaño Estimado |
|-------|---------------|---------------|---------------|-----------------|
| ticket | 25,000 | 750,000 | 9,000,000 | ~2.5 GB/año |
| mensaje | 75,000 | 2,250,000 | 27,000,000 | ~5.4 GB/año |
| audit_log | 100,000 | 3,000,000 | 36,000,000 | ~7.2 GB/año |
| advisor | 5 | 5 | 5 | < 1 MB |

**Total estimado:** ~15 GB/año para 1 sucursal

### Partitioning Strategy (Futuro)

**Para tablas grandes (>10M registros):**
```sql
-- Particionar audit_log por mes
CREATE TABLE audit_log_2024_12 PARTITION OF audit_log
FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Particionar ticket por año  
CREATE TABLE ticket_2024 PARTITION OF ticket
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

---

## 🔧 Maintenance

### Daily Tasks (Automated)

**Cleanup de datos antiguos:**
```sql
-- Eliminar tickets completados > 90 días
DELETE FROM ticket 
WHERE status IN ('COMPLETADO', 'CANCELADO', 'NO_ATENDIDO') 
AND completed_at < NOW() - INTERVAL '90 days';

-- Eliminar mensajes enviados > 30 días
DELETE FROM mensaje 
WHERE estado_envio = 'ENVIADO' 
AND fecha_envio < NOW() - INTERVAL '30 days';

-- Archivar audit_log > 1 año
INSERT INTO audit_log_archive SELECT * FROM audit_log 
WHERE created_at < NOW() - INTERVAL '1 year';
DELETE FROM audit_log WHERE created_at < NOW() - INTERVAL '1 year';
```

**Reset de contadores diarios:**
```sql
-- Reset contadores de asesores a medianoche
UPDATE advisor SET 
  total_tickets_served_today = 0,
  average_service_time_minutes = 0
WHERE created_at::date < CURRENT_DATE;
```

### Weekly Tasks

**Reindex para performance:**
```sql
REINDEX INDEX CONCURRENTLY idx_ticket_created_at;
REINDEX INDEX CONCURRENTLY idx_audit_created_at;
```

**Estadísticas de tablas:**
```sql
ANALYZE ticket;
ANALYZE mensaje;
ANALYZE audit_log;
```

### Monitoring Queries

**Tamaño de tablas:**
```sql
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Índices más utilizados:**
```sql
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

**Queries lentas:**
```sql
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

---

## 🚨 Backup & Recovery

### Backup Strategy

**Daily Full Backup:**
```bash
pg_dump -h localhost -U ticketero_user -d ticketero \
  --format=custom --compress=9 \
  --file=ticketero_$(date +%Y%m%d).backup
```

**Continuous WAL Archiving:**
```bash
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backup/wal/%f'
```

### Recovery Procedures

**Point-in-time Recovery:**
```bash
# Restaurar backup base
pg_restore -h localhost -U ticketero_user -d ticketero_recovery \
  ticketero_20241215.backup

# Aplicar WAL logs hasta punto específico
pg_ctl start -D /data/postgresql \
  -o "-c recovery_target_time='2024-12-15 14:30:00'"
```

---

**Última actualización:** Diciembre 2024  
**Versión del Schema:** 3 (V3__create_mensaje_audit_tables.sql)