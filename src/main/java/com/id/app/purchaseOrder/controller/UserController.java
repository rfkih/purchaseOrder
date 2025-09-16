package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.UserDto;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.services.UserService;
import com.id.app.purchaseOrder.utils.ResponseCode;
import com.id.app.purchaseOrder.utils.ResponseService;
import com.id.app.purchaseOrder.utils.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Value("${application.code}")
    private String applicationCode;
    private final UserService userService;


    @GetMapping
    public ResponseService getUsers(@RequestParam(value = "id", required = false) Integer id) {
        if (id != null) {
            UserDto dto = userService.findById(id);
            if (dto != null) {
                return ResponseUtil.setResponse(
                        HttpStatus.OK.value(),
                        applicationCode,
                        ResponseCode.SUCCESS.getCode(),
                        dto
                        ).getBody();
            }else {
                throw new DataAccessException("User %d not found".formatted(id));
            }
        }
        List<UserDto> all = userService.findAll();
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                all
                 ).getBody();
    }

    @PostMapping
    public ResponseService create(@Validated @RequestBody UserDto dto) {
        UserDto userDto = userService.create(dto);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                userDto
        ).getBody();
    }

    @PutMapping("/{id}")
    public ResponseService update(@PathVariable Integer id,
                                          @Validated @RequestBody UserDto dto) {

        UserDto userDto = userService.update(id, dto);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                userDto
        ).getBody();
    }

    @DeleteMapping("/{id}")
    public ResponseService delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                "message : User %d deleted".formatted(id)
                ).getBody();
    }
}

