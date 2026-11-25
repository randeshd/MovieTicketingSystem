package com.movieticketing.model;

public class Seat {

    private int seatId;
    private char rowLetter;
    private int seatNumber;

    public Seat(int seatId, char rowLetter, int seatNumber) {
        this.seatId = seatId;
        this.rowLetter = rowLetter;
        this.seatNumber = seatNumber;
    }

    /** 'C' + 5 -> "C5" */
    public String getLabel() {
        return "" + rowLetter + seatNumber;
    }

    @Override
    public String toString() { return getLabel(); }

    public int getSeatId() { return seatId; }
    public char getRowLetter() { return rowLetter; }
    public int getSeatNumber() { return seatNumber; }
}