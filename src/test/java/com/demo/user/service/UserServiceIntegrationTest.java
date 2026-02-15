package com.demo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.DuplicateEmailException;
import com.demo.common.exception.InvalidCredentialsException;
import com.demo.common.exception.UnauthorizedException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.UserDeliveryStatusResponse;
import com.demo.order.service.OrderService;
import com.demo.product.dto.CreateProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.service.ProductService;
import com.demo.user.dto.CreateAdminRequest;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.LoginResponse;
import com.demo.user.dto.UserOrdersResponse;
import com.demo.user.entity.Role;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "테스트유저";
    private static final String TEST_PASSWORD = "password1234";

    @Nested
    @DisplayName("회원가입 (registerUser)")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 회원가입 → 유저 생성 성공")
        void registerUser_success() {
            CreateUserRequest request = new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD);

            CreateUserResponse response = userService.registerUser(request);

            assertThat(response.userId()).isNotNull();
            assertThat(response.name()).isEqualTo(TEST_NAME);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("중복 이메일로 가입 시도 → DuplicateEmailException 발생")
        void registerUser_duplicateEmail_throwsException() {
            userService.registerUser(new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD));

            assertThatThrownBy(() ->
                    userService.registerUser(new CreateUserRequest("다른이름", TEST_EMAIL, "pass5678"))
            ).isInstanceOf(DuplicateEmailException.class);
        }
    }

    @Nested
    @DisplayName("어드민 가입 (registerAdmin)")
    class RegisterAdminTest {

        private static final String CORRECT_SECRET = "ADMIN_SECRET_2026";

        @Test
        @DisplayName("올바른 시크릿 키로 어드민 가입 → 성공")
        void registerAdmin_success() {
            CreateAdminRequest request = new CreateAdminRequest(
                    "관리자", "admin@example.com", "admin1234", CORRECT_SECRET
            );

            CreateUserResponse response = userService.registerAdmin(request);

            assertThat(response.userId()).isNotNull();
            assertThat(response.email()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("잘못된 시크릿 키 → UnauthorizedException 발생")
        void registerAdmin_wrongSecretKey_throwsException() {
            CreateAdminRequest request = new CreateAdminRequest(
                    "관리자", "admin@example.com", "admin1234", "WRONG_KEY"
            );

            assertThatThrownBy(() -> userService.registerAdmin(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("어드민도 중복 이메일 불가 → DuplicateEmailException 발생")
        void registerAdmin_duplicateEmail_throwsException() {
            userService.registerAdmin(new CreateAdminRequest(
                    "관리자1", "admin@example.com", "admin1234", CORRECT_SECRET
            ));

            assertThatThrownBy(() ->
                    userService.registerAdmin(new CreateAdminRequest(
                            "관리자2", "admin@example.com", "admin5678", CORRECT_SECRET
                    ))
            ).isInstanceOf(DuplicateEmailException.class);
        }
    }

    @Nested
    @DisplayName("로그인 (login)")
    class LoginTest {

        @BeforeEach
        void setUp() {
            userService.registerUser(new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD));
        }

        @Test
        @DisplayName("정상 로그인 → 유저 정보 반환")
        void login_success() {
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            LoginResponse response = userService.login(request);

            assertThat(response.userId()).isNotNull();
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.name()).isEqualTo(TEST_NAME);
            assertThat(response.role()).isEqualTo(Role.ROLE_USER);
            assertThat(response.message()).isEqualTo("로그인 성공");
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → InvalidCredentialsException")
        void login_wrongEmail_throwsException() {
            LoginRequest request = new LoginRequest("wrong@example.com", TEST_PASSWORD);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 → InvalidCredentialsException")
        void login_wrongPassword_throwsException() {
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("유저 주문 조회 (getUserOrders)")
    class GetUserOrdersTest {

        @Test
        @DisplayName("주문이 없는 유저 → 빈 주문 리스트 반환")
        void getUserOrders_noOrders_returnsEmptyList() {
            CreateUserResponse user = userService.registerUser(
                    new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD)
            );

            UserOrdersResponse response = userService.getUserOrders(user.userId());

            assertThat(response.userId()).isEqualTo(user.userId());
            assertThat(response.userName()).isEqualTo(TEST_NAME);
            assertThat(response.totalOrders()).isZero();
            assertThat(response.orders()).isEmpty();
        }

        @Test
        @DisplayName("주문이 있는 유저 → 주문 리스트 반환 (최신순 정렬)")
        void getUserOrders_withOrders_returnsOrderList() {
            CreateUserResponse user = userService.registerUser(
                    new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD)
            );

            ProductResponse product1 = productService.addProduct(
                    new CreateProductRequest("노트북", 1500000, 50)
            );
            ProductResponse product2 = productService.addProduct(
                    new CreateProductRequest("키보드", 100000, 50)
            );

            orderService.placeOrder(new CreateOrderRequest(user.userId(), product1.productId(), 1, "서울시 강남구"));
            orderService.placeOrder(new CreateOrderRequest(user.userId(), product2.productId(), 2, "서울시 서초구"));

            UserOrdersResponse response = userService.getUserOrders(user.userId());

            assertThat(response.totalOrders()).isEqualTo(2);
            assertThat(response.orders()).hasSize(2);
            assertThat(response.orders())
                    .extracting("productName")
                    .containsExactly("키보드", "노트북");
        }

        @Test
        @DisplayName("존재하지 않는 유저 ID → UserNotFoundException")
        void getUserOrders_userNotFound_throwsException() {
            assertThatThrownBy(() -> userService.getUserOrders(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("어드민 전체 유저 배송 상태 조회 (getAllUsersDeliveryStatus)")
    class GetAllUsersDeliveryStatusTest {

        @Test
        @DisplayName("유저들의 배송 상태를 전체 조회 → 유저별 배송 정보 반환")
        void getAllUsersDeliveryStatus_success() {
            CreateUserResponse user1 = userService.registerUser(
                    new CreateUserRequest("유저1", "user1@example.com", "pass1234")
            );
            CreateUserResponse user2 = userService.registerUser(
                    new CreateUserRequest("유저2", "user2@example.com", "pass1234")
            );

            ProductResponse product = productService.addProduct(
                    new CreateProductRequest("노트북", 1500000, 50)
            );

            orderService.placeOrder(new CreateOrderRequest(user1.userId(), product.productId(), 1, "서울시 강남구"));
            orderService.placeOrder(new CreateOrderRequest(user2.userId(), product.productId(), 2, "서울시 서초구"));

            List<UserDeliveryStatusResponse> result = userService.getAllUsersDeliveryStatus();

            assertThat(result).hasSizeGreaterThanOrEqualTo(2);

            UserDeliveryStatusResponse user1Status = result.stream()
                    .filter(r -> r.userId().equals(user1.userId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(user1Status.userName()).isEqualTo("유저1");
            assertThat(user1Status.totalOrders()).isEqualTo(1);
            assertThat(user1Status.deliveries()).hasSize(1);

            UserDeliveryStatusResponse user2Status = result.stream()
                    .filter(r -> r.userId().equals(user2.userId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(user2Status.userName()).isEqualTo("유저2");
            assertThat(user2Status.totalOrders()).isEqualTo(1);
            assertThat(user2Status.deliveries()).hasSize(1);
        }
    }
}
