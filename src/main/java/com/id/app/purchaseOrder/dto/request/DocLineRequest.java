package com.id.app.purchaseOrder.dto.request;

import jakarta.validation.constraints.NotNull;

public record DocLineRequest(
        @NotNull Integer itemId,
        @NotNull Integer itemQty,
        @NotNull Integer itemCost,
        @NotNull Integer itemPrice
) {}
