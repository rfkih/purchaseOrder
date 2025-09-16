package com.id.app.purchaseOrder.exthandler;

public class InvalidTransactionException extends Exception {
    public InvalidTransactionException(String msg) {
        super(msg);
    }
}
