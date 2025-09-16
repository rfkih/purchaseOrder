package com.id.app.purchaseOrder.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private Integer id;
    @NotBlank
    @Size(max=500)
    private String firstName;
    @Size(max=500)
    private String lastName;
    @Email
    @NotBlank
    private String email;
    private String phone;
}
