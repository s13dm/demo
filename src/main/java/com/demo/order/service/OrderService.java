package com.demo.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.InsufficientStockException;
import com.demo.common.exception.OrderNotFoundException;
import com.demo.common.exception.ProductNotFoundException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.entity.DeliveryStatus;
import com.demo.order.entity.Order;
import com.demo.order.repository.OrderRepository;
import com.demo.product.entity.Product;
import com.demo.product.repository.ProductRepository;
import com.demo.user.entity.User;
import com.demo.user.repository.UserRepository;

@Service
@Transactional
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public CreateOrderResponse placeOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        /*
         * 비관적 락(PESSIMISTIC_WRITE)으로 상품을 조회한다.
         * SELECT ... FOR UPDATE가 실행되어, 동일 상품에 대한 동시 주문 요청이
         * 순차적으로 처리되도록 보장한다.
         * → 재고 차감의 정합성을 보장하여 초과 판매(over-selling)를 방지한다.
         */
        Product product = productRepository.findByIdWithPessimisticLock(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        if (product.getStock() < request.quantity()) {
            throw new InsufficientStockException(product.getName(), product.getStock());
        }

        product.decreaseStock(request.quantity());

        Order order = orderRepository.save(new Order(
                user,
                product,
                request.quantity(),
                request.shippingAddress()
        ));

        return new CreateOrderResponse(
                order.getId(),
                user.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getShippingAddress(),
                order.getDeliveryStatus(),
                order.getOrderedAt()
        );
    }

    @Transactional(readOnly = true)
    public DeliveryStatusResponse checkDeliveryStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }

    public DeliveryStatusResponse updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.changeDeliveryStatus(request.deliveryStatus());

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }

    public DeliveryStatusResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getDeliveryStatus() != DeliveryStatus.ORDERED) {
            throw new IllegalStateException(
                    "주문 취소는 ORDERED 상태에서만 가능합니다. 현재 상태: " + order.getDeliveryStatus()
            );
        }

        order.changeDeliveryStatus(DeliveryStatus.CANCELLED);
        order.getProduct().increaseStock(order.getQuantity());

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }
}
