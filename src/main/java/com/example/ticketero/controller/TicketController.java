package com.example.ticketero.controller;

import com.example.ticketero.model.dto.request.TicketRequest;
import com.example.ticketero.model.dto.response.TicketByRutResponse;
import com.example.ticketero.model.dto.response.TicketPositionResponse;
import com.example.ticketero.model.dto.response.TicketResponse;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        log.info("Creating ticket for customer: {}", request.nationalId());
        TicketResponse response = ticketService.create(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{codigoReferencia}")
    public ResponseEntity<TicketResponse> getByUuid(@PathVariable UUID codigoReferencia) {
        return ticketService.findByCodigoReferencia(codigoReferencia)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{numero}/position")
    public ResponseEntity<TicketPositionResponse> getPosition(@PathVariable String numero) {
        TicketPositionResponse response = ticketService.getPosition(numero);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-rut/{nationalId}")
    public ResponseEntity<TicketByRutResponse> getByRut(@PathVariable String nationalId) {
        TicketByRutResponse response = ticketService.findByRut(nationalId);
        return ResponseEntity.ok(response);
    }
}