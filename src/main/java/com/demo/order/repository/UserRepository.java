package com.demo.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.order.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
