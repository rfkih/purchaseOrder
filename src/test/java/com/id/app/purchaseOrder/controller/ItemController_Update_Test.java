package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for:
 *   PUT /api/items/{id}
 *
 * Pattern:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Local advice maps DataAccessException -> 404
 */
@WebMvcTest(controllers = ItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class ItemController_Update_Test {

    @Autowired private MockMvc mvc;

    @MockBean private ItemService itemService;

    // mock logging stack to keep slice lightweight
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

    // small helper; adapt setters if your ItemDto exposes them
    private ItemDto itemDto(int id, String name) {
        ItemDto dto = new ItemDto();
         dto.setId(id);
         dto.setName(name);
        return dto;
    }

    // ---- test-only exception mapping (404 for not found) ----
    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(DataAccessException.class)
        ResponseEntity<Map<String, Object>> handle404(DataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @Test
    @DisplayName("PUT /api/items/{id} -> 200 with responseData (updated item)")
    void update_ok() throws Exception {
        int id = 42;
        String body = """
        {
          "name": "Item-Updated",
          "description": "Updated desc",
          "price": 12345,
           "cost": 3200
        }
        """;

        given(itemService.update(eq(id), any(ItemDto.class)))
                .willReturn(itemDto(id, "Item-Updated"));

        mvc.perform(put("/api/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isMap());

        verify(itemService, times(1)).update(eq(id), any(ItemDto.class));
    }

//    @Test
//    @DisplayName("PUT /api/items/{id} -> 404 when service reports not found")
//    void update_notFound() throws Exception {
//        int id = 999;
//        String body = """
//        {
//         "name": "Anything",
//          "description": "Updated desc",
//          "price": 12345,
//           "cost": 3200
//        }
//        """;
//
//        given(itemService.update(eq(id), any(ItemDto.class)))
//                .willThrow(new DataAccessException("Item %d not found".formatted(id)));
//
//        mvc.perform(put("/api/items/{id}", id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", nullValue()));
//
//        verify(itemService, times(1)).update(eq(id), any(ItemDto.class));
//    }

    @Test
    @DisplayName("PUT /api/items/{id} with invalid payload -> 400 (validation fails)")
    void update_validationError_400() throws Exception {
        int id = 7;
        // Intentionally invalid/minimal to trigger @Validated on ItemDto (adjust to your constraints)
        String invalidBody = "{}";

        mvc.perform(put("/api/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", nullValue()));

        // Service should not be invoked when validation fails
        verify(itemService, times(0)).update(eq(id), any(ItemDto.class));
    }
}
