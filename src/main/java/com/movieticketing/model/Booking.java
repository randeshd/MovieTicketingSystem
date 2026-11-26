package com.movieticketing.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    public static final int HOLD_MINUTES = 5;
    public static final int MAX_SEATS = 10;

    private int bookingId;
    private String reference;
    private Customer customer;   // set for online bookings
    private Cashier cashier;     // set for walk-in sales
    private Showtime showtime;
    private List<Ticket> tickets = new ArrayList<>();
    private Payment payment;
    private BookingStatus status = BookingStatus.HELD;
    private LocalDateTime bookingTime;
    private LocalDateTime holdExpiresAt;
    private double totalAmount;

    public Booking(String reference, Showtime showtime) {
        this.reference = reference;
        this.showtime = showtime;
        this.bookingTime = LocalDateTime.now();
        this.holdExpiresAt = bookingTime.plusMinutes(HOLD_MINUTES);
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        totalAmount = calculateTotal();
    }

    public double calculateTotal() {
        double total = 0;
        for (Ticket t : tickets) {
            total += t.getPrice();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public boolean isHoldExpired() {
        return status == BookingStatus.HELD && LocalDateTime.now().isAfter(holdExpiresAt);
    }

    public void confirm(Payment payment) {
        this.payment = payment;
        this.status = BookingStatus.CONFIRMED;
    }

    public boolean isOnline() {
        return customer != null;
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getReference() { return reference; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Cashier getCashier() { return cashier; }
    public void setCashier(Cashier cashier) { this.cashier = cashier; }
    public Showtime getShowtime() { return showtime; }
    public List<Ticket> getTickets() { return tickets; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}