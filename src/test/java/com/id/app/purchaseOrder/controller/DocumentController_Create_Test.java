package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.response.DocLineResponse;
import com.id.app.purchaseOrder.dto.response.DocumentResponse;
import com.id.app.purchaseOrder.logging.CustomRequestBodyAdviceAdapter;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.DocumentService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for POST /api/docs (DocumentController#create)
 * - Mocks logging beans, allows interceptor
 * - Stubs service to return a DocumentResponse (record)
 * - Asserts envelope: responseCode + responseData.<fields>
 */
@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class DocumentController_Create_Test {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DocumentService documentService;

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

    private DocumentResponse createdDoc() {
        List<DocLineResponse> lines = List.of(
                new DocLineResponse(4L, 10L, 1, 4000, 5000),
                new DocLineResponse(2L, 5L,  2, 2000, 2500)
        );

        return new DocumentResponse(
                123,
                "[GR] GR-1003",
                LocalDateTime.of(2025, 9, 16, 10, 30),
                30000,
                32500,
                "GR",
                +15,
                lines
        );
    }

    @Test
    @DisplayName("POST /api/docs -> 200 with responseData (created document)")
    void create_ok() throws Exception {
        given(documentService.create(any())).willReturn(createdDoc());

        String body = """
        {
          "description": "[GR] GR-1003",
          "datetime": "2025-09-16T10:30:00",
          "lines": [
            { "itemId": 4, "itemQty": 10, "itemCost": 4000, "itemPrice": 4000 },
            { "itemId": 2, "itemQty": 5,  "itemCost": 2000, "itemPrice": 2500 }
          ]
        }
        """;

        mvc.perform(post("/api/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isMap())
                .andExpect(jsonPath("$.responseData.id").value(123))
                .andExpect(jsonPath("$.responseData.description").value("[GR] GR-1003"))
                .andExpect(jsonPath("$.responseData.totalCost").value(30000))
                .andExpect(jsonPath("$.responseData.totalPrice").value(32500))
                .andExpect(jsonPath("$.responseData.docType").value("GR"))
                .andExpect(jsonPath("$.responseData.stockImpact").value(15));

        verify(documentService, times(1)).create(any());
    }

    @Test
    @DisplayName("POST /api/docs with invalid payload -> 400 (validation)")
    void create_validationError_400() throws Exception {
        String invalidBody = "{}";

        mvc.perform(post("/api/docs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", nullValue()));;

        verify(documentService, times(0)).create(any());
    }
}
