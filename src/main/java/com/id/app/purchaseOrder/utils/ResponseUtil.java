package com.id.app.purchaseOrder.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Builder helpers for ResponseService envelopes. */
public final class ResponseUtil {

    private ResponseUtil() {}

    /** Matches: setResponse(HttpStatus.OK.value(), appCode, code, desc, data) */
    public static <T> ResponseEntity<ResponseService> setResponse(
            int httpStatus,
            String applicationCode,
            Object responseCode,
            Object responseDesc,
            T responseData
    ) {
        ResponseService body = new ResponseService(responseCode, responseDesc, responseData);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpStatus);
        if (applicationCode != null && !applicationCode.isBlank()) {
            builder.header("X-Application-Code", applicationCode);
        }
        return builder.body(body);
    }
}
