package com.id.app.purchaseOrder.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {
    private Integer id;
    @NotBlank @Size(max=500)
    private String name;
    @Size(max=500)
    private String description;
    @NotNull
    private Integer price;
    @NotNull
    private Integer cost;
}
