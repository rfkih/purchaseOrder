package com.id.app.purchaseOrder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateItemRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Integer price,
        @NotNull @Min(0) Integer cost
) {}
