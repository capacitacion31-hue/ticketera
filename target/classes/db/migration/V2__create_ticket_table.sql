-- V2__create_ticket_table.sql
-- Tabla principal de tickets

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

-- Índices para performance
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_assigned_advisor ON ticket(assigned_advisor_id);
CREATE INDEX idx_ticket_position ON ticket(position_in_queue);
CREATE INDEX idx_ticket_queue_status ON ticket(queue_type, status);

-- Comentarios para documentación
COMMENT ON TABLE ticket IS 'Tickets de atención en sucursales';
COMMENT ON COLUMN ticket.codigo_referencia IS 'UUID único para referencias externas';
COMMENT ON COLUMN ticket.numero IS 'Número visible del ticket (C01, P15, etc.)';
COMMENT ON COLUMN ticket.national_id IS 'RUT/ID nacional del cliente';
COMMENT ON COLUMN ticket.telefono IS 'Número de teléfono para notificaciones Telegram';
COMMENT ON COLUMN ticket.queue_type IS 'Tipo de cola: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA';
COMMENT ON COLUMN ticket.status IS 'Estado actual del ticket';
COMMENT ON COLUMN ticket.position_in_queue IS 'Posición actual en cola (calculada en tiempo real)';
COMMENT ON COLUMN ticket.estimated_wait_minutes IS 'Tiempo estimado de espera en minutos';
COMMENT ON COLUMN ticket.assigned_at IS 'Timestamp cuando pasó a ATENDIENDO';
COMMENT ON COLUMN ticket.completed_at IS 'Timestamp cuando pasó a COMPLETADO';
COMMENT ON COLUMN ticket.actual_service_time_minutes IS 'Tiempo real de atención (completedAt - assignedAt)';