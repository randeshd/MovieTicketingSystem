package com.movieticketing.model;

import com.movieticketing.exception.PaymentException;
import java.time.LocalDateTime;

public abstract class Payment {

    private int paymentId;
    private double amount;
    private LocalDateTime paymentTime;
    private PaymentStatus status;

    protected Payment(double amount) {
        this.amount = amount;
    }

    public abstract void process() throws PaymentException;

    /** "CARD" or "CASH", the value of payments.payment_method */
    public abstract String getMethod();

    public abstract boolean isAtCounter();

    protected void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
        this.paymentTime = LocalDateTime.now();
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}