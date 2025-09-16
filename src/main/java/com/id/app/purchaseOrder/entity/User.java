package com.id.app.purchaseOrder.entity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "users")
public class User extends Audit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="first_name", length = 500, nullable = false)
    private String firstName;

    @Column(name="last_name", length = 500)
    private String lastName;

    @Column(name="email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name="phone", length = 255)
    private String phone;
}

