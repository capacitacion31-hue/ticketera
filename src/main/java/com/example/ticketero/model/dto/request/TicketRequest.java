package com.example.ticketero.model.dto.request;

import com.example.ticketero.model.enums.QueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TicketRequest(
    @NotBlank(message = "El RUT/ID es obligatorio")
    @Size(min = 8, max = 20, message = "RUT/ID debe tener entre 8-20 caracteres")
    String nationalId,

    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Teléfono debe tener formato +56XXXXXXXXX")
    String telefono,

    @NotBlank(message = "Sucursal es obligatoria")
    @Size(max = 100, message = "Sucursal máximo 100 caracteres")
    String branchOffice,

    @NotNull(message = "Tipo de cola es obligatorio")
    QueueType queueType
) {}