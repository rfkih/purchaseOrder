package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.DocumentDto;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for:
 *   GET /api/docs/{id}
 *
 * Pattern:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Local advice maps DataAccessException -> 404
 */
@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class DocumentController_GetDocument_Test {

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

    @RestControllerAdvice
    static class TestExceptionAdvice {
        @ExceptionHandler(DataAccessException.class)
        ResponseEntity<Map<String, Object>> handle404(DataAccessException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    private DocumentDto doc(long id) {
        return new DocumentDto(
                id,
                "GR",
                "[GR] GR-1003",
                LocalDateTime.of(2025, 9, 16, 10, 30),
                32500,
                30000,
                null
        );
    }

    @Test
    @DisplayName("GET /api/docs/{id} -> 200 with responseData (document)")
    void getDocument_ok() throws Exception {
        long id = 123L;
        given(documentService.getDocument(id)).willReturn(doc(id));

        mvc.perform(get("/api/docs/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isMap())
                .andExpect(jsonPath("$.responseData.id").value((int) id))
                .andExpect(jsonPath("$.responseData.type").value("GR"))
                .andExpect(jsonPath("$.responseData.description").value("[GR] GR-1003"))
                .andExpect(jsonPath("$.responseData.totalPrice").value(32500))
                .andExpect(jsonPath("$.responseData.totalCost").value(30000));

        verify(documentService, times(1)).getDocument(id);
    }

    @Test
    @DisplayName("GET /api/docs/{id} -> 404 when not found (service throws DataAccessException)")
    void getDocument_notFound() throws Exception {
        long id = 999L;
        given(documentService.getDocument(id))
                .willThrow(new DataAccessException("Document %d not found".formatted(id)));

        mvc.perform(get("/api/docs/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document 999 not found"));

        verify(documentService, times(1)).getDocument(id);
    }
}
