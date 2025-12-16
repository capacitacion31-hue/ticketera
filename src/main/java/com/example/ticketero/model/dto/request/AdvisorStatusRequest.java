package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.AdvisorStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdvisorStatusRequest(
    @NotNull(message = "Estado es obligatorio")
    AdvisorStatus status,

    @Size(max = 200, message = "Razón máximo 200 caracteres")
    String reason
) {}