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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.id.app.purchaseOrder.mapper.Mapper.toDto;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final PoDRepository poDRepository;

    public List<ItemDto> findAll() {
        return itemRepository.findAll()
                .stream().map(Mapper::toDto).collect(Collectors.toList());
    }

    public ItemDto findById(int id) {
        return itemRepository.findByIdAndStatus(id, "ACTIVE")
                .map(Mapper::toDto)
                .orElseThrow(() -> new DataAccessException("Item %d not found".formatted(id)));
    }

    public ItemDto create(CreateItemRequest req) {

        if (itemRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new InvalidInputException("Item name already exists");
        }
        var e = new Item();
        e.setName(req.name().trim());
        e.setDescription(req.description());
        e.setPrice(req.price());
        e.setCost(req.cost());
        e.setStatus("ACTIVE"); // soft-delete default

        var saved = itemRepository.save(e);
        return toDto(saved);
    }

    @Transactional
    public ItemDto update(Integer id, ItemDto dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new DataAccessException("Item %d not found".formatted(id)));

        if (dto.getName() == null || dto.getName().isBlank())
            throw new BadRequestException( "name is required");
        if (dto.getPrice() == null || dto.getPrice() < 0)
            throw new BadRequestException( "price must be >= 0");
        if (dto.getCost() == null || dto.getCost() < 0)
            throw new BadRequestException("cost must be >= 0");

        item.setName(dto.getName().trim());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setCost(dto.getCost());
        item.setUpdatedBy("SYSTEM");

        return toDto(itemRepository.save(item));
    }

    public Item deactivate(int id) throws InvalidTransactionException {
        Item e = itemRepository.findById(id)
                .orElseThrow(() -> new DataAccessException("Item %d not found".formatted(id)));

        if (poDRepository.existsByItemId(id)) throw new InvalidTransactionException("Item used in documents");

        if ("INACTIVE".equals(e.getStatus())) return e;
        e.setStatus("INACTIVE");
        itemRepository.save(e);
        return e;
    }

    public Item activate(int id) {
        Item e = itemRepository.findById(id)
                .orElseThrow(() -> new DataAccessException("Item %d not found".formatted(id)));
        if ("ACTIVE".equals(e.getStatus())) return e;
        e.setStatus("ACTIVE");
        itemRepository.save(e);
        return e;

    }
}

