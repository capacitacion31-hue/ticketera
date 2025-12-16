package com.example.ticketero.exception;

public class ActiveTicketExistsException extends RuntimeException {
    private final String activeTicketNumber;

    public ActiveTicketExistsException(String nationalId, String activeTicketNumber) {
        super("Cliente " + nationalId + " ya tiene un ticket activo: " + activeTicketNumber);
        this.activeTicketNumber = activeTicketNumber;
    }

    public String getActiveTicketNumber() {
        return activeTicketNumber;
    }
}