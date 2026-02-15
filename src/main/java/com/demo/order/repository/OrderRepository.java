package com.demo.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
