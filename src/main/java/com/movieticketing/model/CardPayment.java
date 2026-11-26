package com.movieticketing.model;

import com.movieticketing.exception.PaymentException;

public class CardPayment extends Payment {

    private String cardLast4;
    private boolean atCounter;

    public CardPayment(double amount, String cardLast4, boolean atCounter) {
        super(amount);
        this.cardLast4 = cardLast4;
        this.atCounter = atCounter;
    }

    /** No real bank: online we only check the last 4 digits look right. */
    @Override
    public void process() throws PaymentException {
        if (!atCounter && (cardLast4 == null || !cardLast4.matches("\\d{4}"))) {
            setStatus(PaymentStatus.FAILED);
            throw new PaymentException("Enter the last 4 digits of the card.");
        }
        markSuccess();
    }

    @Override
    public String getMethod() { return "CARD"; }

    @Override
    public boolean isAtCounter() { return atCounter; }

    public String getCardLast4() { return cardLast4; }
}