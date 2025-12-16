package com.example.ticketero.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String numero) {
        super("No existe ticket con número: " + numero);
    }

    public TicketNotFoundException(Long id) {
        super("No existe ticket con ID: " + id);
    }
}