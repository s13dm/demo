package com.demo.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.OrderNotFoundException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.entity.Order;
import com.demo.order.repository.OrderRepository;
import com.demo.user.entity.User;
import com.demo.user.repository.UserRepository;

@Service
@Transactional
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public CreateOrderResponse placeOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

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
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }

    public DeliveryStatusResponse updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.changeDeliveryStatus(request.deliveryStatus());

        return new DeliveryStatusResponse(order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }
}
