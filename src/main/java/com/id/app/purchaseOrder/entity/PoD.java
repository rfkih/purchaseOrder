package com.id.app.purchaseOrder.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "po_d")
public class PoD {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poh_id", nullable = false)
    private PoH header;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name="item_qty", nullable = false)
    private Integer itemQty;

    @Column(name="item_cost", nullable = false)
    private Integer itemCost;

    @Column(name="item_price", nullable = false)
    private Integer itemPrice;
}

