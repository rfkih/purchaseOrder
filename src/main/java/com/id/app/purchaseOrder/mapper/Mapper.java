package com.id.app.purchaseOrder.mapper;

import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.dto.PoDetailDto;
import com.id.app.purchaseOrder.dto.PoDto;
import com.id.app.purchaseOrder.dto.UserDto;
import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.entity.PoH;
import com.id.app.purchaseOrder.entity.User;

import java.util.stream.Collectors;

public class Mapper {

    // --- User ---
    public static UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .build();
    }

    public static User toEntity(UserDto d) {
        return User.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .build();
    }

    // --- Item ---
    public static ItemDto toDto(Item i) {
        return ItemDto.builder()
                .id(i.getId())
                .name(i.getName())
                .description(i.getDescription())
                .price(i.getPrice())
                .cost(i.getCost())
                .build();
    }

    public static Item toEntity(ItemDto d) {
        return Item.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .price(d.getPrice())
                .cost(d.getCost())
                .build();
    }

    // --- PO ---
    public static PoDto toDto(PoH h) {
        PoDto dto = new PoDto();
        dto.setId(h.getId());
        dto.setDatetime(h.getDatetime());
        dto.setDescription(h.getDescription());
        dto.setTotalPrice(h.getTotalPrice());
        dto.setTotalCost(h.getTotalCost());
        dto.setDetails(h.getDetails().stream().map(d -> {
            PoDetailDto dd = new PoDetailDto();
            dd.setId(d.getId());
            dd.setItemId(d.getItem().getId());
            dd.setItemQty(d.getItemQty());
            dd.setItemCost(d.getItemCost());
            dd.setItemPrice(d.getItemPrice());
            return dd;
        }).collect(Collectors.toList()));
        return dto;
    }
}

