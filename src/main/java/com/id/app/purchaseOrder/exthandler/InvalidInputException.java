package com.id.app.purchaseOrder.exthandler;

public class InvalidInputException extends RuntimeException{
    public InvalidInputException(String message) {
        super(message);
    }
}