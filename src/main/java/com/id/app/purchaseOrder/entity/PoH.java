package com.id.app.purchaseOrder.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "po_h")
public class PoH extends Audit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="datetime", nullable = false)
    private LocalDateTime datetime;

    @Column(length = 500)
    private String description;

    @Column(name="total_price", nullable = false)
    private Integer totalPrice;

    @Column(name="total_cost", nullable = false)
    private Integer totalCost;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PoD> details = new ArrayList<>();
}

