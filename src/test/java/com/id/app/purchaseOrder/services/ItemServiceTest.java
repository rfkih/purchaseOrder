package com.id.app.purchaseOrder.services;

import com.id.app.purchaseOrder.dto.ItemDto;
import com.id.app.purchaseOrder.dto.request.CreateItemRequest;
import com.id.app.purchaseOrder.entity.Item;
import com.id.app.purchaseOrder.exthandler.BadRequestException;
import com.id.app.purchaseOrder.exthandler.DataAccessException;
import com.id.app.purchaseOrder.exthandler.InvalidInputException;
import com.id.app.purchaseOrder.exthandler.InvalidTransactionException;
import com.id.app.purchaseOrder.mapper.Mapper;
import com.id.app.purchaseOrder.repository.ItemRepository;
import com.id.app.purchaseOrder.repository.PoDRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @org.mockito.Mock ItemRepository itemRepository;
    @org.mockito.Mock PoDRepository poDRepository;

    @Test
    void findAll_mapsToDto() {
        var svc = new ItemService(itemRepository, poDRepository);

        Item i1 = item(1, "A", "ACTIVE", 100, 50);
        Item i2 = item(2, "B", "ACTIVE", 200, 120);
        when(itemRepository.findAll()).thenReturn(List.of(i1, i2));

        ItemDto d1 = dto(1, "A", 100, 50, "ACTIVE");
        ItemDto d2 = dto(2, "B", 200, 120, "ACTIVE");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(i1)).thenReturn(d1);
            ms.when(() -> Mapper.toDto(i2)).thenReturn(d2);

            var result = svc.findAll();

            assertThat(result).containsExactly(d1, d2);
            ms.verify(() -> Mapper.toDto(i1));
            ms.verify(() -> Mapper.toDto(i2));
        }
    }

    @Test
    void findById_active_ok() {
        var svc = new ItemService(itemRepository, poDRepository);
        Item i = item(10, "Mouse", "ACTIVE", 150, 90);
        when(itemRepository.findByIdAndStatus(10, "ACTIVE")).thenReturn(Optional.of(i));

        ItemDto expected = dto(10, "Mouse", 150, 90, "ACTIVE");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(i)).thenReturn(expected);

            var got = svc.findById(10);

            assertThat(got).isSameAs(expected);
        }
    }

    @Test
    void findById_notFound_throws() {
        var svc = new ItemService(itemRepository, poDRepository);
        when(itemRepository.findByIdAndStatus(99, "ACTIVE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.findById(99))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Item 99 not found");
    }

    @Test
    void create_ok_savesAndMaps() throws InvalidTransactionException {
        var svc = new ItemService(itemRepository, poDRepository);
        var req = new CreateItemRequest("  Cable  ", "USB-C", 50, 20);

        when(itemRepository.existsByNameIgnoreCase("Cable")).thenReturn(false);

        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item e = inv.getArgument(0);
            e.setId(5);
            return e;
        });

        ItemDto mapped = dto(5, "Cable", 50, 20, "ACTIVE");
        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(any(Item.class))).thenReturn(mapped);

            var res = svc.create(req);

            assertThat(res).isSameAs(mapped);
            verify(itemRepository).existsByNameIgnoreCase("Cable");
            verify(itemRepository).save(argThat(e ->
                    e.getName().equals("Cable")
                            && e.getDescription().equals("USB-C")
                            && e.getPrice() == 50
                            && e.getCost() == 20
                            && e.getStatus().equals("ACTIVE")
            ));
        } catch (InvalidTransactionException e) {
            throw new InvalidTransactionException(e.getMessage());
        }
    }

    @Test
    void create_duplicateName_rejected() {
        var svc = new ItemService(itemRepository, poDRepository);
        var req = new CreateItemRequest("Adapter", "desc", 10, 5);

        when(itemRepository.existsByNameIgnoreCase("Adapter")).thenReturn(true);

        assertThatThrownBy(() -> svc.create(req))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("already exists");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void update_ok_validatesAndSaves() {
        var svc = new ItemService(itemRepository, poDRepository);
        Item existing = item(7, "Old", "ACTIVE", 10, 3);
        when(itemRepository.findById(7)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemDto input = new ItemDto();
        input.setName("  New Name  ");
        input.setDescription("Nice");
        input.setPrice(500);
        input.setCost(300);

        ItemDto mapped = dto(7, "New Name", 500, 300, "ACTIVE");

        try (MockedStatic<Mapper> ms = Mockito.mockStatic(Mapper.class)) {
            ms.when(() -> Mapper.toDto(any(Item.class))).thenReturn(mapped);

            var out = svc.update(7, input);

            assertThat(out).isSameAs(mapped);
            assertThat(existing.getName()).isEqualTo("New Name");
            assertThat(existing.getDescription()).isEqualTo("Nice");
            assertThat(existing.getPrice()).isEqualTo(500);
            assertThat(existing.getCost()).isEqualTo(300);
            assertThat(existing.getUpdatedBy()).isEqualTo("SYSTEM");
        }
    }

    @Test
    void update_validationFails() {
        var svc = new ItemService(itemRepository, poDRepository);
        Item existing = item(2, "X", "ACTIVE", 10, 3);
        when(itemRepository.findById(2)).thenReturn(Optional.of(existing));

        var dtoBlankName = new ItemDto();
        dtoBlankName.setName("  ");
        dtoBlankName.setPrice(1);
        dtoBlankName.setCost(1);

        assertThatThrownBy(() -> svc.update(2, dtoBlankName))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("name is required");

        var dtoNegPrice = new ItemDto();
        dtoNegPrice.setName("abc");
        dtoNegPrice.setPrice(-1);
        dtoNegPrice.setCost(0);

        assertThatThrownBy(() -> svc.update(2, dtoNegPrice))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("price must be >= 0");

        var dtoNegCost = new ItemDto();
        dtoNegCost.setName("abc");
        dtoNegCost.setPrice(0);
        dtoNegCost.setCost(-1);

        assertThatThrownBy(() -> svc.update(2, dtoNegCost))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("cost must be >= 0");
    }

    @Test
    void update_notFound_throws() {
        var svc = new ItemService(itemRepository, poDRepository);
        when(itemRepository.findById(404)).thenReturn(Optional.empty());

        var input = new ItemDto();
        input.setName("N");
        input.setPrice(1);
        input.setCost(1);

        assertThatThrownBy(() -> svc.update(404, input))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Item 404 not found");
    }

    @Test
    void deactivate_usedInDocs_rejected() {
        var svc = new ItemService(itemRepository, poDRepository);
        Item e = item(3, "Pen", "ACTIVE", 5, 2);
        when(itemRepository.findById(3)).thenReturn(Optional.of(e));
        when(poDRepository.existsByItemId(3)).thenReturn(true);

        assertThatThrownBy(() -> svc.deactivate(3))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("used in documents");

        assertThat(e.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void deactivate_ok_setsInactive() throws InvalidTransactionException {
        var svc = new ItemService(itemRepository, poDRepository);
        Item e = item(3, "Pen", "ACTIVE", 5, 2);
        when(itemRepository.findById(3)).thenReturn(Optional.of(e));
        when(poDRepository.existsByItemId(3)).thenReturn(false);

        var out = svc.deactivate(3);

        assertThat(out.getStatus()).isEqualTo("INACTIVE");
        verify(itemRepository).save(e);
    }

    @Test
    void activate_ok_setsActive() {
        var svc = new ItemService(itemRepository, poDRepository);
        Item e = item(4, "Pen", "INACTIVE", 5, 2);
        when(itemRepository.findById(4)).thenReturn(Optional.of(e));

        var out = svc.activate(4);

        assertThat(out.getStatus()).isEqualTo("ACTIVE");
        verify(itemRepository).save(e);
    }

    private static Item item(Integer id, String name, String status, Integer price, Integer cost) {
        Item i = new Item();
        i.setId(id);
        i.setName(name);
        i.setDescription(name + " desc");
        i.setStatus(status);
        i.setPrice(price);
        i.setCost(cost);
        return i;
    }

    private static ItemDto dto(Integer id, String name, Integer price, Integer cost, String status) {
        ItemDto d = new ItemDto();
        d.setId(id);
        d.setName(name);
        d.setDescription(name + " desc");
        d.setPrice(price);
        d.setCost(cost);
        d.setStatus(status);
        return d;
    }
}
