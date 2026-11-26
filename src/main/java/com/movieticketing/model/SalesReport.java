package com.movieticketing.model;

import java.time.LocalDate;

public class SalesReport implements Printable {

    private LocalDate fromDate;
    private LocalDate toDate;
    private double onlineCardTotal;
    private double counterCashTotal;
    private double counterCardTotal;
    private int fullTickets;
    private int halfTickets;

    public SalesReport(LocalDate fromDate, LocalDate toDate, double onlineCardTotal, double counterCashTotal,
                       double counterCardTotal, int fullTickets, int halfTickets) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.onlineCardTotal = onlineCardTotal;
        this.counterCashTotal = counterCashTotal;
        this.counterCardTotal = counterCardTotal;
        this.fullTickets = fullTickets;
        this.halfTickets = halfTickets;
    }

    public double getGrandTotal() {
        return onlineCardTotal + counterCashTotal + counterCardTotal;
    }

    @Override
    public String print() {
        return "Sales report " + fromDate + " to " + toDate + "\n"
             + String.format("Online (card):   Rs. %.2f%n", onlineCardTotal)
             + String.format("Counter (cash):  Rs. %.2f%n", counterCashTotal)
             + String.format("Counter (card):  Rs. %.2f%n", counterCardTotal)
             + String.format("TOTAL:           Rs. %.2f%n", getGrandTotal())
             + "Tickets: " + fullTickets + " full, " + halfTickets + " half";
    }

    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public double getOnlineCardTotal() { return onlineCardTotal; }
    public double getCounterCashTotal() { return counterCashTotal; }
    public double getCounterCardTotal() { return counterCardTotal; }
    public int getFullTickets() { return fullTickets; }
    public int getHalfTickets() { return halfTickets; }
}