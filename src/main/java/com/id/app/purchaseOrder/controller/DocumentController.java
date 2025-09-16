package com.id.app.purchaseOrder.controller;

import com.id.app.purchaseOrder.dto.DocumentDto;
import com.id.app.purchaseOrder.dto.request.CreateDocumentRequest;
import com.id.app.purchaseOrder.dto.response.DocumentResponse;
import com.id.app.purchaseOrder.services.DocumentService;
import com.id.app.purchaseOrder.utils.ResponseCode;
import com.id.app.purchaseOrder.utils.ResponseService;
import com.id.app.purchaseOrder.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

    @Value("${application.code}")
    private String applicationCode;

    private final DocumentService documentService;

    @PostMapping
    public ResponseService create(@Valid @RequestBody CreateDocumentRequest req) {
        DocumentResponse created = documentService.create(req);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                created
        ).getBody();
    }

    @GetMapping("/{id}")
    public ResponseService getDocument(@PathVariable Long id) {
        DocumentDto document = documentService.getDocument(id);

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                document
                ).getBody();
    }

    @GetMapping
    public ResponseService listDocuments(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<DocumentDto> documents = documentService.listDocuments(type, from, to);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                documents
        ).getBody();
    }
}
