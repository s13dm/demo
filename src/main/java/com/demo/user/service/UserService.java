package com.demo.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.DuplicateEmailException;
import com.demo.common.exception.InvalidCredentialsException;
import com.demo.common.exception.UnauthorizedException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.entity.Order;
import com.demo.order.repository.OrderRepository;
import com.demo.user.dto.CreateAdminRequest;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.LoginResponse;
import com.demo.user.dto.UserOrdersResponse;
import com.demo.user.entity.Role;
import com.demo.user.entity.User;
import com.demo.user.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private static final String ADMIN_SECRET_KEY = "ADMIN_SECRET_2026";

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public CreateUserResponse registerUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = userRepository.save(
                new User(request.email(), request.name(), request.password(), Role.ROLE_USER)
        );
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail());
    }

    public CreateUserResponse registerAdmin(CreateAdminRequest request) {
        if (!ADMIN_SECRET_KEY.equals(request.adminSecretKey())) {
            throw new UnauthorizedException("어드민 시크릿 키가 올바르지 않습니다.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = userRepository.save(
                new User(request.email(), request.name(), request.password(), Role.ROLE_ADMIN)
        );
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.getPassword().equals(request.password())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "로그인 성공"
        );
    }

    @Transactional(readOnly = true)
    public UserOrdersResponse getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<Order> orders = orderRepository.findByUserIdOrderByOrderedAtDesc(userId);

        List<CreateOrderResponse> orderResponses = orders.stream()
                .map(order -> new CreateOrderResponse(
                        order.getId(),
                        user.getId(),
                        order.getProductName(),
                        order.getQuantity(),
                        order.getShippingAddress(),
                        order.getDeliveryStatus(),
                        order.getOrderedAt()
                ))
                .toList();

        return new UserOrdersResponse(
                user.getId(),
                user.getName(),
                orderResponses.size(),
                orderResponses
        );
    }
}
