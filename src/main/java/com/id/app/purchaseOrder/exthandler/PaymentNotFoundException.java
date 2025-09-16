package com.id.app.purchaseOrder.exthandler;

public class PaymentNotFoundException extends Exception {
    public PaymentNotFoundException(String msg) {
        super(msg);
    }
}