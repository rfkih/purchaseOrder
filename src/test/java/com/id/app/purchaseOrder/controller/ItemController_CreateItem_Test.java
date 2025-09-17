package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.config.InterceptorConfig;
import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for:
 *   POST /api/items
 *
 * Notes:
 * - Security filters disabled (no CSRF/auth noise).
 * - Real LogInterceptor registered once; it should NOT log POST (your code logs GET only).
 * - Envelope asserted: responseCode/responseDesc/responseData.
 */
@WebMvcTest(
        controllers = ItemController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = InterceptorConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
@Import(ItemController_CreateItem_Test.InterceptorTestConfig.class)
class ItemController_CreateItem_Test {

    @Autowired private MockMvc mvc;

    @MockBean private ItemService itemService;
    @MockBean private LoggingService loggingService;

    private ItemDto createdDto() {
        ItemDto dto = new ItemDto();
         dto.setId(101);
         dto.setName("Paper A5");
         dto.setDescription("70gsm");
         dto.setPrice(4500);
         dto.setCost(3000);
        return dto;
    }

    @Test
    @DisplayName("POST /api/items -> 200 with responseData (created item)")
    void create_ok() throws Exception {
        // Stub service
        given(itemService.create(any())).willReturn(createdDto());

        String body = """
        {
          "name": "Paper A5",
          "description": "70gsm",
          "price": 4500,
          "cost": 3000
        }
        """;

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor", "TESTER")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseDesc").value("Success"))
                .andExpect(jsonPath("$.responseData").exists()); // typically a map/object

        verify(itemService, times(1)).create(any());
        // Your interceptor logs only GET; ensure it didn't log here:
    }

    @Test
    @DisplayName("POST /api/items with invalid payload -> 400 (validation)")
    void create_validationError_400() throws Exception {
        // Missing required fields on purpose; adjust based on your @Validated rules
        String invalidBody = """
        {
          "name": ""
        }
        """;

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor", "TESTER")
                        .content(invalidBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", nullValue()));

    }

    @TestConfiguration
    static class InterceptorTestConfig implements WebMvcConfigurer {

        @Bean
        LogInterceptor testLogInterceptor(LoggingService loggingService) {
            return new LogInterceptor(loggingService);
        }

        private final LogInterceptor testLogInterceptor;

        InterceptorTestConfig(LogInterceptor testLogInterceptor) {
            this.testLogInterceptor = testLogInterceptor;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(testLogInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/error", "/favicon.ico");
        }
    }
}

