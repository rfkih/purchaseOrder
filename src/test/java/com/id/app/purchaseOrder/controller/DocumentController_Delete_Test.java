package com.id.app.purchaseOrder.controller;

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
 *   DELETE /api/docs/{id}
 *
 * Pattern:
 *  - Security filters disabled
 *  - Logging beans mocked + interceptor allowed
 *  - Local advice maps DataAccessException -> 404
 */
@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class DocumentController_Delete_Test {

    @Autowired private MockMvc mvc;

    @MockBean private DocumentService documentService;

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
    @DisplayName("DELETE /api/docs/{id} -> 200 with success envelope and message")
    void delete_ok() throws Exception {
        long id = 123L;
        doNothing().when(documentService).delete(id);

        mvc.perform(delete("/api/docs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"));

        verify(documentService, times(1)).delete(eq(id));
    }

    @Test
    @DisplayName("DELETE /api/docs/{id} -> 404 when document not found")
    void delete_notFound() throws Exception {
        long id = 999L;
        doThrow(new DataAccessException("Document not found: " + id))
                .when(documentService).delete(id);

        mvc.perform(delete("/api/docs/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Document not found: " + id));

        verify(documentService, times(1)).delete(eq(id));
    }
}
