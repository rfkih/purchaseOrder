package com.id.app.purchaseOrder.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "items")
public class Item extends Audit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 500, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(length = 20)
    private String quantity;

    @Column(length = 20)
    private String status;

    @Column(nullable = false)
    private Integer cost;
}

