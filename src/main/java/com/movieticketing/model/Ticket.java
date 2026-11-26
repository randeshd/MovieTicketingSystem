package com.movieticketing.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket implements Printable {

    private int ticketId;
    private Seat seat;
    private Showtime showtime;
    private TicketType type;
    private double price;
    private LocalDateTime printedAt;
    private Cashier printedBy;

    public Ticket(Seat seat, Showtime showtime, TicketType type, double price) {
        this.seat = seat;
        this.showtime = showtime;
        this.type = type;
        this.price = price;
    }

    /** 18+ movie only: the cashier checks ID. (Children don't carry ID, so Half is not checked.) */
    public boolean needsIdCheck() {
        return showtime.getMovie().is18Plus();
    }

    public boolean isPrinted() {
        return printedAt != null;
    }

    public void markPrinted(Cashier cashier) {
        this.printedAt = LocalDateTime.now();
        this.printedBy = cashier;
    }

    @Override
    public String print() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
        return "==== ABC CINEMA ====\n"
             + showtime.getMovie().getTitle() + "\n"
             + showtime.getStartDateTime().format(f) + "\n"
             + "Seat: " + seat.getLabel() + "   " + type + "\n"
             + String.format("Rs. %.2f", price) + "\n"
             + (needsIdCheck() ? "ID CHECK REQUIRED\n" : "")
             + "====================";
    }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public Seat getSeat() { return seat; }
    public Showtime getShowtime() { return showtime; }
    public TicketType getType() { return type; }
    public double getPrice() { return price; }
    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }
    public Cashier getPrintedBy() { return printedBy; }
    public void setPrintedBy(Cashier printedBy) { this.printedBy = printedBy; }
}