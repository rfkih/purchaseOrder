package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.UserDto;
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
 * Slice tests for:
 *   PUT /api/users/{id}
 *
 * Mirrors your existing test setup:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Small test-only advice to map DataAccessException -> 404
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class UserController_UpdateUser_Test {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

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

    private UserDto user(int id, String firstName) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setFirstName(firstName);
        return dto;
    }

    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(DataAccessException.class)
        ResponseEntity<Map<String, Object>> handle404(DataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @Test
    @DisplayName("PUT /api/users/{id} -> 200 with responseData (updated user)")
    void update_ok() throws Exception {
        int id = 7;
        String body = """
        {
          "firstName": "Alice-Updated",
          "email": "Alice-Updated@example.com"
        }
        """;
        given(userService.update(eq(id), any(UserDto.class)))
                .willReturn(user(id, "Alice-Updated"));

        mvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isMap())
                .andExpect(jsonPath("$.responseData.id").value(id))
                .andExpect(jsonPath("$.responseData.firstName").value("Alice-Updated"));

        verify(userService, times(1)).update(eq(id), any(UserDto.class));
    }

    @Test
    @DisplayName("PUT /api/users/{id} -> 404 when service signals not found (throws DataAccessException)")
    void update_notFound() throws Exception {
        int id = 999;
        String body = """
        {
          "firstName": "Nobody",
           "email": "Alice-Updated@example.com"
        }
        """;
        given(userService.update(eq(id), any(UserDto.class)))
                .willThrow(new DataAccessException("User %d not found".formatted(id)));

        mvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User 999 not found"));

        verify(userService, times(1)).update(eq(id), any(UserDto.class));
    }

    @Test
    @DisplayName("PUT /api/users/{id} with invalid payload -> 400 (validation fails)")
    void update_validationError_400() throws Exception {
        int id = 7;

        String invalidBody = "{}";
        mvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", nullValue()));

        verify(userService, times(0)).update(eq(id), any(UserDto.class));
    }
}
