package com.movieticketing.model;

import javax.swing.JFrame;

public class Cashier extends User {

    private int cashierId; // bookings.cashier_id and tickets.printed_by point at this

    public Cashier(int userId, String name, String email, String passwordHash, boolean active, int cashierId) {
        super(userId, name, email, passwordHash, Role.CASHIER, active);
        this.cashierId = cashierId;
    }

    public void printTicket(Ticket ticket) {
        ticket.markPrinted(this);
        System.out.println(ticket.print());
    }

    @Override
    public JFrame createDashboard() {
        return null; // replaced when we build the cashier screen
    }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int cashierId) { this.cashierId = cashierId; }
}