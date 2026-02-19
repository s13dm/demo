package com.demo.user.controller;

import java.util.List;
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
import com.demo.order.dto.UserDeliveryStatusResponse;
import com.demo.user.entity.Role;
import com.demo.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "일반 사용자 회원가입", description = "새로운 일반 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public CreateUserResponse registerUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.registerUser(request);
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "관리자 회원가입", description = "새로운 관리자 계정을 등록합니다. 관리자 시크릿 키가 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "관리자 등록 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터 또는 잘못된 시크릿 키"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public CreateUserResponse registerAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return userService.registerAdmin(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 세션이 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        LoginResponse response = userService.login(request);
        session.setAttribute("userId", response.userId());
        session.setAttribute("role", response.role());
        return response;
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하고 로그아웃합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "로그아웃 성공");
    }

    @GetMapping("/{userId}/orders")
    @Operation(summary = "사용자 주문 목록 조회", description = "특정 사용자의 주문 목록을 조회합니다. 본인 또는 관리자만 접근 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public UserOrdersResponse getUserOrders(
            @Parameter(description = "조회할 사용자 ID", required = true) @PathVariable Long userId,
            HttpSession session) {
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

    @GetMapping("/admin/deliveries")
    @Operation(summary = "전체 사용자 배송 상태 조회 (관리자 전용)", description = "모든 사용자의 배송 상태를 조회합니다. 관리자만 접근 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    public List<UserDeliveryStatusResponse> getAllUsersDeliveryStatus(HttpSession session) {
        Long sessionUserId = (Long) session.getAttribute("userId");
        Role sessionRole = (Role) session.getAttribute("role");

        if (sessionUserId == null) {
            throw new UnauthorizedException();
        }

        if (sessionRole != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("어드민만 접근할 수 있습니다.");
        }

        return userService.getAllUsersDeliveryStatus();
    }
}
