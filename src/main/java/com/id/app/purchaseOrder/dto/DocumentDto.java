package com.id.app.purchaseOrder.dto;

import com.id.app.purchaseOrder.dto.response.DocumentDetailResponse;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
        Long id,
        String type,
        String description,
        LocalDateTime datetime,
        Integer totalPrice,
        Integer totalCost,
        List<DocumentDetailResponse> details
) {}
