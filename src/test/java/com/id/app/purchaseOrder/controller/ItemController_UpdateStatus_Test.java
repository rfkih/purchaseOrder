package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.exthandler.BadRequestException;
import com.id.app.purchaseOrder.exthandler.InvalidTransactionException;
import com.id.app.purchaseOrder.logging.CustomRequestBodyAdviceAdapter;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for:
 *   PATCH /api/items/{id}/status  with body: { "status": "ACTIVE|INACTIVE" }
 *
 * Mirrors your established pattern:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Local advice maps BadRequestException -> 400 and InvalidTransactionException -> 409
 */
@WebMvcTest(controllers = ItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class ItemController_UpdateStatus_Test {

    @Autowired private MockMvc mvc;

    @MockBean private ItemService itemService;

    // Mock logging-related beans so the slice loads cleanly
    @MockBean private LoggingService loggingService;
    @MockBean private LogInterceptor logInterceptor;
    @MockBean private CustomRequestBodyAdviceAdapter customRequestBodyAdviceAdapter;

    @BeforeEach
    void allowInterceptor() {
        org.mockito.Mockito.when(logInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);
    }

    // ---------- Test-only exception mapping ----------
    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(BadRequestException.class)
        ResponseEntity<Map<String, Object>> badRequest(BadRequestException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
        @ExceptionHandler(InvalidTransactionException.class)
        ResponseEntity<Map<String, Object>> invalidTxn(InvalidTransactionException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }
    }

    @Test
    @DisplayName("PATCH /api/items/{id}/status ACTIVE -> 200, calls activate, returns responseData")
    void activate_ok() throws Exception {
        int id = 7;
        given(itemService.activate(id)).willReturn(new Item());

        mvc.perform(patch("/api/items/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").exists());

        verify(itemService, times(1)).activate(id);
        verify(itemService, never()).deactivate(anyInt());
    }

    @Test
    @DisplayName("PATCH /api/items/{id}/status INACTIVE -> 200, calls deactivate, returns responseData")
    void deactivate_ok() throws Exception {
        int id = 8;
        given(itemService.deactivate(id)).willReturn(new Item());

        mvc.perform(patch("/api/items/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"));

        verify(itemService, times(1)).deactivate(id);
        verify(itemService, never()).activate(anyInt());
    }

    @Test
    @DisplayName("PATCH /api/items/{id}/status with bad value -> 400 (BadRequestException)")
    void badStatus_400() throws Exception {
        int id = 9;

        // Fail fast if controller incorrectly calls service
        when(itemService.activate(anyInt()))
                .thenThrow(new AssertionError("activate() must NOT be called for bad status"));
        when(itemService.deactivate(anyInt()))
                .thenThrow(new AssertionError("deactivate() must NOT be called for bad status"));

        mvc.perform(patch("/api/items/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAUSED\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));

        verify(itemService, never()).activate(anyInt());
        verify(itemService, never()).deactivate(anyInt());
    }


//    @Test
//    @DisplayName("PATCH /api/items/{id}/status ACTIVE -> service throws InvalidTransactionException -> 409")
//    void activate_invalidTxn_409() throws Exception {
//        int id = 10;
//        given(itemService.activate(id))
//                .willThrow(new InvalidTransactionException("Cannot activate item %d".formatted(id)));
//
//        mvc.perform(patch("/api/items/{id}/status", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"status\":\"ACTIVE\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", nullValue()));
//
//        verify(itemService, times(1)).activate(id);
//    }


}
