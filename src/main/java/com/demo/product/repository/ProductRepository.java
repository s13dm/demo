package com.demo.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demo.product.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    /**
     * 비관적 락(PESSIMISTIC_WRITE)을 사용하여 상품을 조회한다.
     * SELECT ... FOR UPDATE 쿼리가 실행되어, 다른 트랜잭션이 같은 row를
     * 읽거나 수정하지 못하도록 잠근다.
     * → 동시 주문 시 재고 차감의 정합성을 보장한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
}
