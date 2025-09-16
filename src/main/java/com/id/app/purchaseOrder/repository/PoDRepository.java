package com.id.app.purchaseOrder.repository;

import com.id.app.purchaseOrder.entity.PoD;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoDRepository extends JpaRepository<PoD, Integer> {
    boolean existsByItemId(Integer id);
}

