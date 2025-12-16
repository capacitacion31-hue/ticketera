-- V1__create_advisor_table.sql
-- Tabla de asesores

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

-- Comentarios para documentación
COMMENT ON TABLE advisor IS 'Asesores que atienden clientes en módulos numerados';
COMMENT ON COLUMN advisor.name IS 'Nombre completo del asesor';
COMMENT ON COLUMN advisor.status IS 'Estado: AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN advisor.module_number IS 'Número del módulo asignado (1-5)';
COMMENT ON COLUMN advisor.assigned_tickets_count IS 'Contador de tickets asignados actualmente';
COMMENT ON COLUMN advisor.workload_minutes IS 'Carga de trabajo ponderada actual en minutos';
COMMENT ON COLUMN advisor.average_service_time_minutes IS 'Tiempo promedio real de atención';
COMMENT ON COLUMN advisor.total_tickets_served_today IS 'Total de tickets completados hoy';
COMMENT ON COLUMN advisor.queue_types IS 'Tipos de cola que puede atender (JSON array)';