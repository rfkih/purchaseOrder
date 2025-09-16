package com.id.app.purchaseOrder.dto.response;



public record DocLineResponse(
        Long id,
        Long itemId,
        Integer qty,
        Integer cost,
        Integer price
) {}
