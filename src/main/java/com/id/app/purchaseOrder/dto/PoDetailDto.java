package com.id.app.purchaseOrder.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PoDetailDto {
    private Integer id;
    @NotNull
    private Integer itemId;
    @NotNull
    private Integer itemQty;
    @NotNull
    private Integer itemCost;
    @NotNull
    private Integer itemPrice;
}


