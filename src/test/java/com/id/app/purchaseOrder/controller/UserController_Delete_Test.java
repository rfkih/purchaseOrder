package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.logging.CustomRequestBodyAdviceAdapter;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for:
 *   DELETE /api/users/{id}
 *
 * Pattern:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Local advice maps DataAccessException -> 404
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class UserController_Delete_Test {

    @Autowired private MockMvc mvc;

    @MockBean private UserService userService;

    // mock logging stack so the slice loads cleanly
    @MockBean private LoggingService loggingService;
    @MockBean private LogInterceptor logInterceptor;
    @MockBean private CustomRequestBodyAdviceAdapter customRequestBodyAdviceAdapter;

    @BeforeEach
    void allowInterceptor() {
        when(logInterceptor.preHandle(any(), any(), any())).thenReturn(true);
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
    @DisplayName("DELETE /api/users/{id} -> 200, returns success envelope with message")
    void delete_ok() throws Exception {
        int id = 12;
        // userService.delete is void; doNothing() is default, so no stubbing required
        doNothing().when(userService).delete(id);

        mvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").value("message : User " + id + " deleted"));

        verify(userService, times(1)).delete(eq(id));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} -> 404 when user does not exist")
    void delete_notFound() throws Exception {
        int id = 999;
        doThrow(new DataAccessException("User %d not found".formatted(id)))
                .when(userService).delete(id);

        mvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User 999 not found"));

        verify(userService, times(1)).delete(eq(id));
    }
}
