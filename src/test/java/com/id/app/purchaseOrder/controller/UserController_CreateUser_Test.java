package com.id.app.purchaseOrder.controller;


import com.id.app.purchaseOrder.dto.UserDto;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for:
 *   POST /api/users
 *
 * Mirrors your UserController_GetUsers_Test setup:
 * - Security filters disabled
 * - Logging beans mocked, interceptor allowed
 * - Assert envelope: responseCode + responseData
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class UserController_CreateUser_Test {

    @Autowired
    private MockMvc mvc;

    // Controller dependency
    @MockBean
    private UserService userService;

    // Mock ALL logging-related beans so the slice loads cleanly
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

    // helper to build a returned DTO from service
    private UserDto createdUser(int id, String firstName) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setFirstName(firstName);
        return dto;
    }

    @Test
    @DisplayName("POST /api/users -> 200 with responseData (created user)")
    void create_ok() throws Exception {
        given(userService.create(any(UserDto.class))).willReturn(createdUser(101, "Alice"));

        String body = """
        {
          "firstName": "Alice",
          "email": "Alice@example.com"
        }
        """;

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                // don’t assert responseDesc if your success contract doesn’t require it
                .andExpect(jsonPath("$.responseData").isMap())
                .andExpect(jsonPath("$.responseData.firstName").value("Alice"));

        verify(userService, times(1)).create(any(UserDto.class));
    }

    @Test
    @DisplayName("POST /api/users with invalid payload -> 400 (validation fails)")
    void create_validationError_400() throws Exception {
        String invalidBody = "{}";

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", nullValue()));

        verify(userService, times(0)).create(any(UserDto.class));
    }
}
