-- V3__create_mensaje_audit_tables.sql
-- Tablas de mensajes y auditoría

-- Tabla de mensajes Telegram
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    plantilla VARCHAR(50) NOT NULL CHECK (plantilla IN ('TOTEM_TICKET_CREADO', 'TOTEM_PROXIMO_TURNO', 'TOTEM_ES_TU_TURNO')),
    estado_envio VARCHAR(20) NOT NULL CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de auditoría
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    previous_state JSON,
    new_state JSON,
    additional_data JSON,
    ip_address VARCHAR(45),
    user_agent TEXT
);

-- Índices para performance - Mensaje
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado ON mensaje(estado_envio);
CREATE INDEX idx_mensaje_fecha_programada ON mensaje(fecha_programada);
CREATE INDEX idx_mensaje_plantilla ON mensaje(plantilla);
CREATE INDEX idx_mensaje_intentos ON mensaje(intentos);

-- Índices para performance - Audit Log
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp DESC);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_actor ON audit_log(actor);

-- Comentarios para documentación - Mensaje
COMMENT ON TABLE mensaje IS 'Mensajes de Telegram enviados a clientes';
COMMENT ON COLUMN mensaje.ticket_id IS 'Referencia al ticket asociado';
COMMENT ON COLUMN mensaje.plantilla IS 'Plantilla del mensaje: TOTEM_TICKET_CREADO, TOTEM_PROXIMO_TURNO, TOTEM_ES_TU_TURNO';
COMMENT ON COLUMN mensaje.estado_envio IS 'Estado del envío: PENDIENTE, ENVIADO, FALLIDO';
COMMENT ON COLUMN mensaje.fecha_programada IS 'Cuándo debe enviarse el mensaje';
COMMENT ON COLUMN mensaje.fecha_envio IS 'Cuándo se envió realmente (null si no enviado)';
COMMENT ON COLUMN mensaje.telegram_message_id IS 'ID del mensaje retornado por Telegram API';
COMMENT ON COLUMN mensaje.intentos IS 'Número de intentos de envío (máximo 4)';

-- Comentarios para documentación - Audit Log
COMMENT ON TABLE audit_log IS 'Registro de auditoría de eventos del sistema';
COMMENT ON COLUMN audit_log.event_type IS 'Tipo de evento: TICKET_CREADO, TICKET_ASIGNADO, etc.';
COMMENT ON COLUMN audit_log.actor IS 'Quién ejecutó la acción: cliente, asesor, supervisor, sistema';
COMMENT ON COLUMN audit_log.entity_type IS 'Tipo de entidad afectada: TICKET, ADVISOR, QUEUE';
COMMENT ON COLUMN audit_log.entity_id IS 'Identificador de la entidad afectada';
COMMENT ON COLUMN audit_log.previous_state IS 'Estado anterior de la entidad (JSON)';
COMMENT ON COLUMN audit_log.new_state IS 'Estado nuevo de la entidad (JSON)';
COMMENT ON COLUMN audit_log.additional_data IS 'Información adicional del contexto (JSON)';