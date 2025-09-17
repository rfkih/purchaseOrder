package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.UserDto;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.logging.CustomRequestBodyAdviceAdapter;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Focused slice tests for UserController:
 *   - GET /api/users?id={id}  -> responseData is an object
 *   - GET /api/users          -> responseData is an array
 *
 * Notes:
 * - Security filters disabled to keep the test about controller behavior only.
 * - We MOCK all logging-related beans (LogInterceptor, LoggingService, CustomRequestBodyAdviceAdapter)
 *   so @WebMvcTest doesn't try to build the real ones.
 * - A tiny test-only @RestControllerAdvice converts DataAccessException -> 404 for assertion.
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class UserController_GetUsers_Test {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private LoggingService loggingService;

    @MockBean
    private LogInterceptor logInterceptor;

    @MockBean
    private CustomRequestBodyAdviceAdapter customRequestBodyAdviceAdapter;

    @BeforeEach
    void allowInterceptor() {
        org.mockito.Mockito.when(logInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);
    }

    private UserDto userDto(int id, String firstName) {
        UserDto dto = new UserDto();
         dto.setId(id);
         dto.setFirstName(firstName);
        return dto;
    }

    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(DataAccessException.class)
        ResponseEntity<Map<String, Object>> handle404(DataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", ex.getMessage()
            ));
        }
    }

    @Nested
    class GetById {

        @Test
        @DisplayName("GET /api/users?id=1 -> 200, responseData is an object")
        void getById_ok() throws Exception {
            given(userService.findById(1)).willReturn(userDto(1, "alice"));

            mvc.perform(get("/api/users").param("id", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.responseData").isMap());

            verify(userService).findById(1);
        }

        @Test
        @DisplayName("GET /api/users?id=999 -> 404 when not found")
        void getById_notFound() throws Exception {
            given(userService.findById(999)).willReturn(null);

            mvc.perform(get("/api/users").param("id", "999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User 999 not found"));

            verify(userService).findById(999);
        }
    }

    @Nested
    class GetAll {

        @Test
        @DisplayName("GET /api/users -> 200, responseData is an array")
        void getAll_ok() throws Exception {
            given(userService.findAll()).willReturn(List.of(
                    userDto(1, "alice"),
                    userDto(2, "bob")
            ));

            mvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.responseData").isArray());

            verify(userService).findAll();
        }
    }
}
