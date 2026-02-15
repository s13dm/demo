package com.demo.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
