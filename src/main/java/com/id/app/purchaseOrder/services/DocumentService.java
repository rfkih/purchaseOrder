package com.id.app.purchaseOrder.services;

import com.id.app.purchaseOrder.dto.DocumentDto;
import com.id.app.purchaseOrder.dto.response.DocumentDetailResponse;
import com.id.app.purchaseOrder.dto.request.CreateDocumentRequest;
import com.id.app.purchaseOrder.dto.response.DocLineResponse;

import com.id.app.purchaseOrder.dto.response.DocumentResponse;
import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.entity.PoD;
import com.id.app.purchaseOrder.entity.PoH;
import com.id.app.purchaseOrder.exthandler.BadRequestException;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.repository.ItemRepository;
import com.id.app.purchaseOrder.repository.PoDRepository;
import com.id.app.purchaseOrder.repository.PoHRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final PoHRepository poHRepo;
    private final PoDRepository poDRepo;
    private final ItemRepository itemRepo;

    public DocumentService(PoHRepository poHRepo, PoDRepository poDRepo, ItemRepository itemRepo) {
        this.poHRepo = poHRepo; this.poDRepo = poDRepo; this.itemRepo = itemRepo;
    }

    public DocumentResponse create(CreateDocumentRequest req) {
        String docType = parseType(req.description()); // [PO],[GR],[ADJ_IN],[ADJ_OUT]
        if (req.lines() == null || req.lines().isEmpty())
            throw new BadRequestException("Document must have at least 1 line");

        PoH h = new PoH();
        h.setDescription(req.description());
        h.setDatetime(req.datetime());
        h.setTotalCost(0);
        h.setTotalPrice(0);
        h = poHRepo.save(h);

        int totalCost = 0, totalPrice = 0, qtySum = 0;
        List<PoD> savedLines = new ArrayList<>();

        for (var l : req.lines()) {
            if (l.itemQty() == null || l.itemQty() <= 0)
                throw new BadRequestException( "itemQty must be > 0");
            if (l.itemCost() < 0 || l.itemPrice() < 0)
                throw new BadRequestException( "itemCost/itemPrice must be >= 0");

            Item item = itemRepo.findByIdAndStatus(l.itemId(), "ACTIVE")
                    .orElseThrow(() -> new BadRequestException(
                            "Item %d not found or inactive".formatted(l.itemId())));

            PoD d = new PoD();
            d.setHeader(h);
            d.setItem(item);
            d.setItemQty(l.itemQty());
            d.setItemCost(l.itemCost());
            d.setItemPrice(l.itemPrice());
            savedLines.add(poDRepo.save(d));

            totalCost  += l.itemCost()  * l.itemQty();
            totalPrice += l.itemPrice() * l.itemQty();
            qtySum     += l.itemQty();
        }

        h.setTotalCost(totalCost);
        h.setTotalPrice(totalPrice);
        h = poHRepo.save(h);

        int stockImpact = switch (docType) {
            case "GR", "ADJ_IN" -> qtySum;
            case "ADJ_OUT"      -> -qtySum;
            default             -> 0;
        };

        List<DocLineResponse> lineDtos = savedLines.stream()
                .map(d -> new DocLineResponse(
                        Long.parseLong(String.valueOf(d.getId())),
                        Long.parseLong(String.valueOf(d.getItem().getId())),
                        d.getItemQty(),
                        d.getItemCost(),
                        d.getItemPrice()
                ))
                .toList();

        return new DocumentResponse(
                h.getId(), h.getDescription(), h.getDatetime(),
                h.getTotalCost(), h.getTotalPrice(),
                docType, stockImpact, lineDtos
        );
    }

    private String parseType(String description) {
        String d = description == null ? "" : description.trim().toUpperCase();
        if (d.startsWith("[PO]")) return "PO";
        if (d.startsWith("[GR]")) return "GR";
        if (d.startsWith("[ADJ_IN]")) return "ADJ_IN";
        if (d.startsWith("[ADJ_OUT]")) return "ADJ_OUT";
        throw new BadRequestException(
                "Unknown document type tag. Use [PO], [GR], [ADJ_IN], or [ADJ_OUT].");
    }

    public DocumentDto getDocument(Long id) {
        PoH h = poHRepo.findById(Math.toIntExact(id))
                .orElseThrow(() -> new DataAccessException("Document not found: " + id));

        return toDto(h);
    }

    public List<DocumentDto> listDocuments(String type, LocalDateTime from, LocalDateTime to) {
        return poHRepo.findByFilter(type, from, to)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private DocumentDto toDto(PoH h) {
        List<DocumentDetailResponse> detailDTOs = h.getDetails().stream()
                .map(d -> new DocumentDetailResponse(
                        Long.parseLong(String.valueOf(d.getId())),
                        Long.parseLong(String.valueOf(d.getItem().getId())),
                        d.getItem().getName(),
                        d.getItemQty(),
                        d.getItemCost(),
                        d.getItemPrice()
                ))
                .toList();

        return new DocumentDto(
                Long.parseLong(String.valueOf(h.getId())),
                h.getDescription(), // using description as "type"
                h.getDescription(),
                h.getDatetime(),
                h.getTotalPrice(),
                h.getTotalCost(),
                detailDTOs
        );
    }


}
