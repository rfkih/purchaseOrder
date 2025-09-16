package com.id.app.purchaseOrder.exthandler;

public class BadRequestException  extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

