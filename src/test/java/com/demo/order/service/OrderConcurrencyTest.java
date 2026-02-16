package com.demo.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.demo.order.dto.CreateOrderRequest;
import com.demo.product.dto.CreateProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.service.ProductService;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.service.UserService;

/*
 * ================================================
 * 동시성 테스트 (Concurrency Test)
 * ================================================
 *
 * [왜 @Transactional을 붙이지 않는가?]
 * - @Transactional이 테스트에 붙으면 모든 작업이 하나의 트랜잭션에서 실행된다.
 * - 동시성 테스트는 여러 스레드가 각각 독립적인 트랜잭션을 사용해야 의미가 있다.
 * - @Transactional을 붙이면 비관적 락 테스트가 불가능하다 (같은 트랜잭션이므로 잠금 경합 없음).
 *
 * [CountDownLatch란?]
 * - 여러 스레드가 동시에 시작하도록 동기화하는 장치.
 * - latch.countDown(): 카운트를 1 감소시킨다.
 * - latch.await(): 카운트가 0이 될 때까지 대기한다.
 *
 * [테스트 전략]
 * - 재고 100개인 상품에 100명이 동시에 1개씩 주문한다.
 * - 비관적 락이 정상 동작하면: 성공 100건, 재고 0개.
 * - 락이 없으면: 동시 읽기로 인해 일부 주문이 같은 재고를 읽어서
 *   실제 차감된 재고보다 더 많은 주문이 성공할 수 있다 (초과 판매).
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("100명이 동시에 재고 100개 상품을 1개씩 주문 → 정확히 100개 성공, 재고 0개")
    void concurrentOrders_pessimisticLock_stockConsistency() throws InterruptedException {
        // given: 유저 등록 및 재고 100개 상품 등록
        CreateUserResponse user = userService.registerUser(
                new CreateUserRequest("동시성테스트유저", "concurrency@example.com", "pass1234")
        );
        Long userId = user.userId();

        ProductResponse product = productService.addProduct(
                new CreateProductRequest("동시성테스트상품", 10000, 100)
        );
        Long productId = product.productId();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100개의 스레드가 동시에 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.placeOrder(
                            new CreateOrderRequest(userId, productId, 1, "서울시 강남구")
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 재고가 정확히 0이어야 한다
        ProductResponse result = productService.getProduct(productId);

        assertThat(successCount.get()).isEqualTo(100);
        assertThat(failCount.get()).isZero();
        assertThat(result.stock()).isZero();
    }

    @Test
    @DisplayName("재고 10개 상품에 100명이 동시 주문 → 정확히 10개만 성공, 90개 실패")
    void concurrentOrders_insufficientStock_onlyAvailableSucceed() throws InterruptedException {
        // given: 재고 10개 상품 등록
        CreateUserResponse user = userService.registerUser(
                new CreateUserRequest("재고부족테스트유저", "stock-test@example.com", "pass1234")
        );
        Long userId = user.userId();

        ProductResponse product = productService.addProduct(
                new CreateProductRequest("한정판상품", 50000, 10)
        );
        Long productId = product.productId();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 100개의 스레드가 동시에 주문
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.placeOrder(
                            new CreateOrderRequest(userId, productId, 1, "서울시 서초구")
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then: 정확히 10개만 성공하고 재고는 0
        ProductResponse result = productService.getProduct(productId);

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(90);
        assertThat(result.stock()).isZero();
    }
}
