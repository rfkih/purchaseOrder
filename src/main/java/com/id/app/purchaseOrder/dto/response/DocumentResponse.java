package com.id.app.purchaseOrder.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Integer id,
        String description,
        LocalDateTime datetime,
        Integer totalCost,
        Integer totalPrice,
        String docType,
        Integer stockImpact, // +N/-N/0 depending on tag
        List<DocLineResponse> lines
) {}
