package com.id.app.purchaseOrder.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@MappedSuperclass
public abstract class Audit {
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_datetime", nullable = false)
    private LocalDateTime createdDatetime = LocalDateTime.now();

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedDatetime = LocalDateTime.now();
    }
}

