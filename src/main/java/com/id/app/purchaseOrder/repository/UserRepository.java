package com.id.app.purchaseOrder.repository;


import com.id.app.purchaseOrder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}

