package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.config.InterceptorConfig;
import static org.hamcrest.Matchers.nullValue;
import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.ItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ItemController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = InterceptorConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"application.code=30062"})
@Import({ItemController_GetItems_Test.TestAdvice.class,
        ItemController_GetItems_Test.InterceptorTestConfig.class})
class ItemController_GetItems_Test {

    @Autowired MockMvc mvc;

    @MockBean ItemService itemService;
    @MockBean LoggingService loggingService;

    private ItemDto itemDto(int id, String name) {
        ItemDto dto = new ItemDto();
         dto.setId(id); dto.setName(name);
        return dto;
    }

    @Test
    void getById_ok() throws Exception {
        given(itemService.findById(1)).willReturn(itemDto(1, "Paper"));

        mvc.perform(get("/api/items").param("id", "1"))
                .andDo(print())
                .andExpect(jsonPath("$.responseDesc").value("Success"))
                .andExpect(status().isOk());

        verify(itemService).findById(1);
        verify(loggingService, times(1)).logRequest(any(), isNull());
    }

//    @Test
//    void getById_notFound() throws Exception {
//        given(itemService.findById(999)).willReturn(null);
//
//        mvc.perform(get("/api/items").param("id", "999"))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data", nullValue()));
//
//        verify(itemService).findById(999);
//        verify(loggingService, times(1)).logRequest(any(), isNull());
//    }

    @Test
    void getAll_ok() throws Exception {
        given(itemService.findAll()).willReturn(List.of(itemDto(1, "Paper")));

        mvc.perform(get("/api/items"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseData").isArray());

        verify(itemService).findAll();
        verify(loggingService, times(1)).logRequest(any(), isNull());
    }

    @TestConfiguration
    static class InterceptorTestConfig implements WebMvcConfigurer {


        @Bean
        LogInterceptor testLogInterceptor(LoggingService loggingService) {
            return new LogInterceptor(loggingService);
        }

        @Autowired
        private LogInterceptor testLogInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(testLogInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/error", "/favicon.ico");
        }
    }

    @ControllerAdvice
    static class TestAdvice {
        @ExceptionHandler(DataAccessException.class)
        ResponseEntity<Map<String, Object>> handle404(DataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }
}
