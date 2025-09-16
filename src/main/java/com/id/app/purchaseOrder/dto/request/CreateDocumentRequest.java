package com.id.app.purchaseOrder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateDocumentRequest(
        @NotBlank String description,
        @NotNull LocalDateTime datetime,
        @NotNull List<DocLineRequest> lines
) {}