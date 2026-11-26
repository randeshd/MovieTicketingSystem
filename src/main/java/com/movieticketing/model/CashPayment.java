package com.movieticketing.model;

import com.movieticketing.exception.PaymentException;

public class CashPayment extends Payment {

    private double amountReceived;
    private double changeGiven;

    public CashPayment(double amount, double amountReceived) {
        super(amount);
        this.amountReceived = amountReceived;
    }

    @Override
    public void process() throws PaymentException {
        if (amountReceived < getAmount()) {
            setStatus(PaymentStatus.FAILED);
            throw new PaymentException(String.format("Not enough cash. Total is Rs. %.2f", getAmount()));
        }
        changeGiven = Math.round((amountReceived - getAmount()) * 100.0) / 100.0;
        markSuccess();
    }

    @Override
    public String getMethod() { return "CASH"; }

    @Override
    public boolean isAtCounter() { return true; } // cash is always paid at the counter

    public double getAmountReceived() { return amountReceived; }
    public double getChangeGiven() { return changeGiven; }
    public void setChangeGiven(double changeGiven) { this.changeGiven = changeGiven; }
}