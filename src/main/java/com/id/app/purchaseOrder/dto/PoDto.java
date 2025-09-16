package com.id.app.purchaseOrder.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoDto {
    private Integer id;
    @NotNull
    private LocalDateTime datetime;
    @Size(max=500)
    private String description;
    private Integer totalPrice;
    private Integer totalCost;
    @NotNull
    private List<PoDetailDto> details;
}

