package com.id.app.purchaseOrder.services;

import com.id.app.purchaseOrder.dto.DocumentDto;
import com.id.app.purchaseOrder.dto.request.CreateDocumentRequest;
import com.id.app.purchaseOrder.dto.request.DocLineRequest;
import com.id.app.purchaseOrder.dto.response.DocumentResponse;
import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.entity.PoD;
import com.id.app.purchaseOrder.entity.PoH;
import com.id.app.purchaseOrder.exthandler.BadRequestException;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.repository.ItemRepository;
import com.id.app.purchaseOrder.repository.PoDRepository;
import com.id.app.purchaseOrder.repository.PoHRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests: no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock PoHRepository poHRepo;
    @Mock PoDRepository poDRepo;
    @Mock ItemRepository itemRepo;

    @InjectMocks DocumentService service;

    private Item activeItem1;
    private Item activeItem2;

    @BeforeEach
    void setUp() {
        activeItem1 = new Item();
        // assuming your entity IDs are Integer; if Long, set accordingly
        activeItem1.setId(10);
        activeItem1.setName("Item A");
        activeItem1.setStatus("ACTIVE");

        activeItem2 = new Item();
        activeItem2.setId(20);
        activeItem2.setName("Item B");
        activeItem2.setStatus("ACTIVE");
    }

    @Test
    void create_gr_success_calculatesTotalsAndStockImpact() {
        // Arrange
        var req = new CreateDocumentRequest(
                "[GR] Goods receipt for PO-123",
                LocalDateTime.parse("2025-01-10T12:00:00"),
                List.of(
                        new DocLineRequest(10, 2, 100, 120), // itemId, qty, cost, price
                        new DocLineRequest(20, 3, 200, 250)
                )
        );

        // poH save -> assign ID on first save and return; on second save return with totals
        when(poHRepo.save(any(PoH.class))).thenAnswer(inv -> {
            PoH h = inv.getArgument(0);
            if (h.getId() == null) h.setId(1); // first save assigns ID
            return h;
        });

        when(itemRepo.findByIdAndStatus(10, "ACTIVE")).thenReturn(Optional.of(activeItem1));
        when(itemRepo.findByIdAndStatus(20, "ACTIVE")).thenReturn(Optional.of(activeItem2));

        // poD save -> assign IDs to lines
        when(poDRepo.save(any(PoD.class))).thenAnswer(inv -> {
            PoD d = inv.getArgument(0);
            if (d.getId() == null) d.setId((int)(Math.random() * 1000 + 1)); // any int id
            return d;
        });

        // Act
        DocumentResponse resp = service.create(req);

        // Assert totals: totalCost = 2*100 + 3*200 = 800; totalPrice = 2*120 + 3*250 = 990
        assertThat(resp.totalCost()).isEqualTo(800);
        assertThat(resp.totalPrice()).isEqualTo(990);
        // GR => stockImpact = + (2 + 3) = +5
        assertThat(resp.stockImpact()).isEqualTo(5);
        assertThat(resp.docType()).isEqualTo("GR");
        assertThat(resp.lines()).hasSize(2);
        assertThat(resp.id()).isNotNull();

        // Verify saves
        verify(poHRepo, times(2)).save(any(PoH.class)); // header saved twice (before & after totals)
        verify(poDRepo, times(2)).save(any(PoD.class));
    }

    @Test
    void create_adjOut_success_negativeStockImpact() {
        var req = new CreateDocumentRequest(
                "[ADJ_OUT] Stock write-off",
                LocalDateTime.parse("2025-03-01T09:00:00"),
                List.of(new DocLineRequest(10, 4, 0, 0))
        );
        when(poHRepo.save(any(PoH.class))).thenAnswer(inv -> {
            PoH h = inv.getArgument(0);
            if (h.getId() == null) h.setId(2);
            return h;
        });
        when(itemRepo.findByIdAndStatus(10, "ACTIVE")).thenReturn(Optional.of(activeItem1));
        when(poDRepo.save(any(PoD.class))).thenAnswer(inv -> { PoD d = inv.getArgument(0); d.setId(200); return d; });

        var resp = service.create(req);

        assertThat(resp.docType()).isEqualTo("ADJ_OUT");
        assertThat(resp.stockImpact()).isEqualTo(-4);
        assertThat(resp.totalCost()).isEqualTo(0);
        assertThat(resp.totalPrice()).isEqualTo(0);
    }

    @Test
    void create_rejects_emptyLines() {
        var req = new CreateDocumentRequest("[PO] Create", LocalDateTime.now(), List.of());
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 1 line");
        verifyNoInteractions(itemRepo, poDRepo);
    }

    @Test
    void create_rejects_invalidQtyOrPrices() {
        var badQty = new CreateDocumentRequest(
                "[GR] oops", LocalDateTime.now(), List.of(new DocLineRequest(10, 0, 10, 10)));
        assertThatThrownBy(() -> service.create(badQty))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("itemQty must be > 0");

        var badCost = new CreateDocumentRequest(
                "[GR] oops", LocalDateTime.now(), List.of(new DocLineRequest(10, 1, -1, 10)));
        assertThatThrownBy(() -> service.create(badCost))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("itemCost/itemPrice must be >= 0");
    }

    @Test
    void create_rejects_inactiveItem() {
        var req = new CreateDocumentRequest(
                "[GR] test", LocalDateTime.now(), List.of(new DocLineRequest(99, 1, 10, 10)));
        when(itemRepo.findByIdAndStatus(99, "ACTIVE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found or inactive");
    }

    @Test
    void getDocument_ok_mapsToDto() {
        PoH h = new PoH();
        h.setId(5);
        h.setDescription("[PO] Office supplies");
        h.setDatetime(LocalDateTime.parse("2025-02-02T10:00:00"));
        h.setTotalCost(300);
        h.setTotalPrice(450);

        PoD d = new PoD();
        d.setId(501);
        d.setHeader(h);
        d.setItem(activeItem1);
        d.setItemQty(3);
        d.setItemCost(50);
        d.setItemPrice(75);
        h.setDetails(List.of(d));

        when(poHRepo.findById(5)).thenReturn(Optional.of(h));

        DocumentDto dto = service.getDocument(5L);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.type()).isEqualTo("[PO] Office supplies"); // you’re using description as "type" in toDto
        assertThat(dto.details()).hasSize(1);
        assertThat(dto.details().get(0).itemName()).isEqualTo("Item A");
    }

    @Test
    void getDocument_notFound_throws() {
        when(poHRepo.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDocument(999L))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void listDocuments_ok() {
        PoH h1 = new PoH(); h1.setId(1); h1.setDescription("PO"); h1.setDatetime(LocalDateTime.now());
        PoH h2 = new PoH(); h2.setId(2); h2.setDescription("GR"); h2.setDatetime(LocalDateTime.now());
        when(poHRepo.findByFilter(eq("PO"), any(), any())).thenReturn(List.of(h1));

        var list = service.listDocuments("PO", null, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).type()).isEqualTo("PO");
    }
}