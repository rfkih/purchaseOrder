package com.id.app.purchaseOrder.repository;
import com.id.app.purchaseOrder.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    Page<Item> findByStatus(String status, Pageable pageable);
    Optional<Item> findByIdAndStatus(Integer id, String status);
    boolean existsByNameIgnoreCase(String name);
}