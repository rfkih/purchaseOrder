package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.dto.request.CreateItemRequest;
import com.id.app.purchaseOrder.dto.request.StatusRequest;
import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.exthandler.BadRequestException;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.exthandler.InvalidTransactionException;
import com.id.app.purchaseOrder.services.ItemService;
import com.id.app.purchaseOrder.utils.ResponseCode;
import com.id.app.purchaseOrder.utils.ResponseService;
import com.id.app.purchaseOrder.utils.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    @Value("${application.code}")
    private String applicationCode;

    private final ItemService itemService;

    @GetMapping
    public ResponseService getItems(@RequestParam(value = "id", required = false) Integer id) {
        if (id != null) {
            ItemDto dto = itemService.findById(id);
            if (dto != null) {
                return ResponseUtil.setResponse(
                        HttpStatus.OK.value(),
                        applicationCode,
                        ResponseCode.SUCCESS.getCode(),
                        ResponseCode.SUCCESS.getDescription(),
                        dto
                    ).getBody();
            }else {
                throw new DataAccessException("Item %d not found".formatted(id));
            }
        }
        List<ItemDto> all = itemService.findAll();
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                all
            ).getBody();
    }

    @PostMapping
    public ResponseService create(@Validated @RequestBody CreateItemRequest request) {

        ItemDto resp = itemService.create(request);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
                ).getBody();
    }

    @PutMapping("/{id}")
    public ResponseService update(@PathVariable Integer id,
                              @Validated @RequestBody ItemDto dto) {
        ItemDto resp = itemService.update(id, dto);

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
                ).getBody();
    }

    @PatchMapping("/{id}/status")
    public ResponseService updateStatus(@PathVariable int id, @RequestBody StatusRequest body) throws InvalidTransactionException {
        Item item;
        if ("INACTIVE".equalsIgnoreCase(body.status())) item = itemService.deactivate(id);
        else if ("ACTIVE".equalsIgnoreCase(body.status())) item = itemService.activate(id);
        else throw new BadRequestException("status must be ACTIVE or INACTIVE");

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                item
            ).getBody();
    }

}

