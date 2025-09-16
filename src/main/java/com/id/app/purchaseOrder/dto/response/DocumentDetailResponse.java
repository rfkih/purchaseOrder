package com.id.app.purchaseOrder.dto.response;

// DocumentDetailDTO.java
public record DocumentDetailResponse(
        Long id,
        Long itemId,
        String itemName,
        Integer qty,
        Integer cost,
        Integer price
) {

}


