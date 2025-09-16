package com.id.app.purchaseOrder.utils;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseService {
    Object responseCode;
    Object responseDesc;
    Object responseData;

    public ResponseService() {
    }

    public ResponseService(Object responseCode, Object responseDesc, Object responseData) {
        this.responseCode = responseCode;
        this.responseDesc = responseDesc;
        this.responseData = responseData;
    }

}