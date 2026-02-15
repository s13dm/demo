package com.demo.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.CreateUserRequest;
import com.demo.order.dto.CreateUserResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.entity.Order;
import com.demo.order.entity.User;
import com.demo.order.repository.OrderRepository;
import com.demo.order.repository.UserRepository;

@Service
@Transactional
public class OrderDeliveryService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderDeliveryService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public CreateUserResponse registerUser(CreateUserRequest request) {
        User user = userRepository.save(new User(request.email(), request.name()));
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail());
    }

    public CreateOrderResponse placeOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + request.userId()));

        Order order = orderRepository.save(new Order(
                user,
                request.productName(),
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
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id=" + orderId));

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }

    public DeliveryStatusResponse updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id=" + orderId));

        order.changeDeliveryStatus(request.deliveryStatus());

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }
}
