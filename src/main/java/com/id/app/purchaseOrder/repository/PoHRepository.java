package com.id.app.purchaseOrder.repository;

import com.id.app.purchaseOrder.entity.PoH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PoHRepository extends JpaRepository<PoH, Integer> {
    @Query("""
    SELECT h FROM PoH h
    WHERE (:type IS NULL OR h.description = :type)
      AND h.createdDatetime >= COALESCE(:from, h.createdDatetime)
      AND h.createdDatetime <= COALESCE(:to, h.createdDatetime)
    ORDER BY h.createdDatetime DESC
""")
    List<PoH> findByFilter(
            @Param("type") String type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
