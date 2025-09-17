package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.DocumentDto;
import com.id.app.purchaseOrder.logging.CustomRequestBodyAdviceAdapter;
import com.id.app.purchaseOrder.logging.LogInterceptor;
import com.id.app.purchaseOrder.logging.LoggingService;
import com.id.app.purchaseOrder.services.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for:
 *   GET /api/docs?type={type}&from={isoDateTime}&to={isoDateTime}
 *
 * Contract:
 *   - Returns envelope with responseCode + responseData (array)
 *   - Optional filters; bad datetime format -> 400
 */
@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = { "application.code=30062" })
class DocumentController_ListDocuments_Test {

    @Autowired MockMvc mvc;

    @MockBean DocumentService documentService;

    @MockBean LoggingService loggingService;
    @MockBean LogInterceptor logInterceptor;
    @MockBean CustomRequestBodyAdviceAdapter customRequestBodyAdviceAdapter;

    @BeforeEach
    void allowInterceptor() {
        when(logInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }


    private DocumentDto doc(long id, String type, String desc, int totalPrice, int totalCost) {
        return new DocumentDto(
                id,
                type,
                desc,
                LocalDateTime.of(2025, 9, 16, 10, 30),
                totalPrice,
                totalCost,
                null
        );
    }

    @Test
    @DisplayName("GET /api/docs (no params) -> 200, returns list")
    void list_noParams_ok() throws Exception {
        given(documentService.listDocuments(null, null, null))
                .willReturn(List.of(
                        doc(1L, "GR", "[GR] GR-1001", 10000, 9000),
                        doc(2L, "GI", "[GI] GI-2001",  5000, 4500)
                ));

        mvc.perform(get("/api/docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isArray())
                .andExpect(jsonPath("$.responseData.length()").value(2))
                .andExpect(jsonPath("$.responseData[0].type").value("GR"))
                .andExpect(jsonPath("$.responseData[1].type").value("GI"));

        verify(documentService, times(1)).listDocuments(null, null, null);
    }

    @Test
    @DisplayName("GET /api/docs with all params -> 200, passes filters to service")
    void list_withParams_ok() throws Exception {
        String type = "GR";
        String fromStr = "2025-09-15T00:00:00";
        String toStr   = "2025-09-17T23:59:59";

        given(documentService.listDocuments(eq(type), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(doc(3L, "GR", "[GR] GR-1003", 32500, 30000)));

        mvc.perform(get("/api/docs")
                        .param("type", type)
                        .param("from", fromStr)
                        .param("to", toStr)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.responseData").isArray())
                .andExpect(jsonPath("$.responseData[0].id").value(3))
                .andExpect(jsonPath("$.responseData[0].type").value("GR"))
                .andExpect(jsonPath("$.responseData[0].description").value("[GR] GR-1003"));


        ArgumentCaptor<LocalDateTime> fromCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCap   = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(documentService).listDocuments(eq(type), fromCap.capture(), toCap.capture());


        LocalDateTime expectedFrom = LocalDateTime.parse(fromStr);
        LocalDateTime expectedTo   = LocalDateTime.parse(toStr);
        assert expectedFrom.equals(fromCap.getValue());
        assert expectedTo.equals(toCap.getValue());
    }

    @Test
    @DisplayName("GET /api/docs with bad datetime -> 400 (format error)")
    void list_badDate_400() throws Exception {
        mvc.perform(get("/api/docs")
                        .param("from", "2025-09-16 10:30:00") // not ISO; missing 'T'
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data", nullValue()));

        verify(documentService, times(0)).listDocuments(any(), any(), any());
    }
}
