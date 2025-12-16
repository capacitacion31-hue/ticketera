package com.example.ticketero.model.enums;

public enum EstadoEnvio {
    PENDIENTE("Pendiente de envío"),
    ENVIADO("Enviado exitosamente"),
    FALLIDO("Falló después de reintentos");

    private final String description;

    EstadoEnvio(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}