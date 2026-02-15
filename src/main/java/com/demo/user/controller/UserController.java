package com.demo.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.exception.UnauthorizedException;
import com.demo.user.dto.CreateAdminRequest;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.LoginResponse;
import com.demo.user.dto.UserOrdersResponse;
import com.demo.user.entity.Role;
import com.demo.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse registerUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.registerUser(request);
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse registerAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return userService.registerAdmin(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        LoginResponse response = userService.login(request);
        session.setAttribute("userId", response.userId());
        session.setAttribute("role", response.role());
        return response;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "로그아웃 성공");
    }

    @GetMapping("/{userId}/orders")
    public UserOrdersResponse getUserOrders(@PathVariable Long userId, HttpSession session) {
        Long sessionUserId = (Long) session.getAttribute("userId");
        Role sessionRole = (Role) session.getAttribute("role");

        if (sessionUserId == null) {
            throw new UnauthorizedException();
        }

        if (!sessionUserId.equals(userId) && sessionRole != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("본인의 주문만 조회할 수 있습니다.");
        }

        return userService.getUserOrders(userId);
    }
}
