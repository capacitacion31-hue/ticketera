package com.example.ticketero.model.enums;

public enum MessageTemplate {
    TOTEM_TICKET_CREADO("Confirmación de creación", "INMEDIATO"),
    TOTEM_PROXIMO_TURNO("Pre-aviso", "CUANDO_POSICION_3"),
    TOTEM_ES_TU_TURNO("Turno activo", "AL_ASIGNAR");

    private final String description;
    private final String triggerMoment;

    MessageTemplate(String description, String triggerMoment) {
        this.description = description;
        this.triggerMoment = triggerMoment;
    }

    public String getDescription() { return description; }
    public String getTriggerMoment() { return triggerMoment; }
}