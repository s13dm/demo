package com.demo.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * ================================================
 * @SpringBootTest 통합 테스트 핵심 정리
 * ================================================
 *
 * [@SpringBootTest]
 * - 실제 Spring ApplicationContext를 로드하여 모든 Bean을 등록한다.
 * - @WebMvcTest와 달리 Service, Repository, DB 연결까지 전부 실제로 동작한다.
 * - 즉, Mock 없이 실제 DB(여기서는 H2 인메모리)에 접근하는 통합 테스트다.
 *
 * [@Transactional on Test]
 * - 테스트 메서드 실행 후 자동으로 롤백(rollback)된다.
 * - 따라서 각 테스트가 서로 DB 상태에 영향을 주지 않는다.
 * - 주의: @Transactional이 테스트에 붙으면 "기본이 롤백"이다.
 *        프로덕션 코드의 @Transactional과 다르게 동작하므로 헷갈리지 말 것.
 *
 * [H2 인메모리 DB]
 * - MODE=MYSQL: H2가 MySQL 호환 모드로 동작한다.
 * - ddl-auto: create-drop → 테스트 시작 시 테이블 생성, 종료 시 삭제.
 * - 실제 MySQL 없이도 JPA + Repository 통합 테스트가 가능하다.
 */

import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.DuplicateEmailException;
import com.demo.common.exception.InvalidCredentialsException;
import com.demo.common.exception.UnauthorizedException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.service.OrderService;
import com.demo.user.dto.CreateAdminRequest;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.dto.LoginRequest;
import com.demo.user.dto.LoginResponse;
import com.demo.user.dto.UserOrdersResponse;
import com.demo.user.entity.Role;

@SpringBootTest       // 실제 ApplicationContext를 전부 로드 (Service, Repository, DB 포함)
@Transactional        // 각 테스트 종료 후 자동 롤백 → DB 상태 격리 보장
class UserServiceIntegrationTest {

    /*
     * @Autowired: 스프링 컨테이너에서 실제 Bean을 주입받는다.
     * Mock이 아닌 진짜 UserService, OrderService가 주입된다.
     */
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    // 테스트에서 공통으로 사용할 상수
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "테스트유저";
    private static final String TEST_PASSWORD = "password1234";

    /*
     * ================================================
     * @Nested: 테스트를 논리적으로 그룹화하는 JUnit 5 기능.
     * - Inner class로 관련 테스트를 묶어서 가독성을 높인다.
     * - @DisplayName과 함께 쓰면 테스트 결과가 계층적으로 출력된다.
     * ================================================
     */
    @Nested
    @DisplayName("회원가입 (registerUser)")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 회원가입 → 유저 생성 성공")
        void registerUser_success() {
            // given
            CreateUserRequest request = new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD);

            // when
            CreateUserResponse response = userService.registerUser(request);

            // then
            /*
             * assertThat(): AssertJ 라이브러리의 핵심 메서드.
             * JUnit의 assertEquals()보다 가독성이 좋고, 체이닝(chaining)으로 여러 검증을 할 수 있다.
             *
             * .isNotNull() → null이 아닌지 확인
             * .isEqualTo() → 값이 같은지 확인
             */
            assertThat(response.userId()).isNotNull();
            assertThat(response.name()).isEqualTo(TEST_NAME);
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("중복 이메일로 가입 시도 → DuplicateEmailException 발생")
        void registerUser_duplicateEmail_throwsException() {
            // given: 먼저 한 번 가입
            userService.registerUser(new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD));

            // when & then
            /*
             * assertThatThrownBy(): 예외 발생을 검증하는 AssertJ 메서드.
             * - 람다 안의 코드가 실행될 때 예외가 던져지는지 확인한다.
             * - .isInstanceOf()로 예외 타입을 검증할 수 있다.
             * - JUnit의 assertThrows()와 유사하지만 체이닝이 가능하다.
             */
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
            // given
            CreateAdminRequest request = new CreateAdminRequest(
                    "관리자", "admin@example.com", "admin1234", CORRECT_SECRET
            );

            // when
            CreateUserResponse response = userService.registerAdmin(request);

            // then
            assertThat(response.userId()).isNotNull();
            assertThat(response.email()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("잘못된 시크릿 키 → UnauthorizedException 발생")
        void registerAdmin_wrongSecretKey_throwsException() {
            // given
            CreateAdminRequest request = new CreateAdminRequest(
                    "관리자", "admin@example.com", "admin1234", "WRONG_KEY"
            );

            // when & then
            assertThatThrownBy(() -> userService.registerAdmin(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("어드민도 중복 이메일 불가 → DuplicateEmailException 발생")
        void registerAdmin_duplicateEmail_throwsException() {
            // given
            userService.registerAdmin(new CreateAdminRequest(
                    "관리자1", "admin@example.com", "admin1234", CORRECT_SECRET
            ));

            // when & then
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

        /*
         * @BeforeEach: 해당 @Nested 클래스 안의 각 @Test 실행 전에 호출된다.
         * - 로그인 테스트를 위해 매번 유저를 미리 등록해둔다.
         * - @Transactional 덕분에 테스트 후 롤백되므로 매번 새로 등록해도 충돌 없음.
         */
        @BeforeEach
        void setUp() {
            userService.registerUser(new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD));
        }

        @Test
        @DisplayName("정상 로그인 → 유저 정보 반환")
        void login_success() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            // when
            LoginResponse response = userService.login(request);

            // then
            assertThat(response.userId()).isNotNull();
            assertThat(response.email()).isEqualTo(TEST_EMAIL);
            assertThat(response.name()).isEqualTo(TEST_NAME);
            assertThat(response.role()).isEqualTo(Role.ROLE_USER);
            assertThat(response.message()).isEqualTo("로그인 성공");
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → InvalidCredentialsException")
        void login_wrongEmail_throwsException() {
            // given
            LoginRequest request = new LoginRequest("wrong@example.com", TEST_PASSWORD);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 → InvalidCredentialsException")
        void login_wrongPassword_throwsException() {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");

            // when & then
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
            // given: 유저만 등록하고 주문은 안 함
            CreateUserResponse user = userService.registerUser(
                    new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD)
            );

            // when
            UserOrdersResponse response = userService.getUserOrders(user.userId());

            // then
            assertThat(response.userId()).isEqualTo(user.userId());
            assertThat(response.userName()).isEqualTo(TEST_NAME);
            assertThat(response.totalOrders()).isZero();
            assertThat(response.orders()).isEmpty();  // .isEmpty(): 컬렉션이 비어있는지 검증
        }

        @Test
        @DisplayName("주문이 있는 유저 → 주문 리스트 반환 (최신순 정렬)")
        void getUserOrders_withOrders_returnsOrderList() {
            // given
            CreateUserResponse user = userService.registerUser(
                    new CreateUserRequest(TEST_NAME, TEST_EMAIL, TEST_PASSWORD)
            );

            // 주문 2건 등록 (OrderService를 통해 실제 DB에 저장)
            orderService.placeOrder(new CreateOrderRequest(user.userId(), "노트북", 1, "서울시 강남구"));
            orderService.placeOrder(new CreateOrderRequest(user.userId(), "키보드", 2, "서울시 서초구"));

            // when
            UserOrdersResponse response = userService.getUserOrders(user.userId());

            // then
            assertThat(response.totalOrders()).isEqualTo(2);
            assertThat(response.orders()).hasSize(2);  // .hasSize(): 컬렉션 크기 검증

            /*
             * extracting(): 객체 리스트에서 특정 필드만 추출하여 검증.
             * - 예: orders 리스트에서 productName만 뽑아서 "키보드", "노트북"이 포함되어 있는지 확인.
             * containsExactly(): 순서까지 정확히 일치하는지 검증.
             * - 최신순(Desc) 정렬이므로 "키보드"가 먼저 나와야 한다.
             */
            assertThat(response.orders())
                    .extracting("productName")
                    .containsExactly("키보드", "노트북");
        }

        @Test
        @DisplayName("존재하지 않는 유저 ID → UserNotFoundException")
        void getUserOrders_userNotFound_throwsException() {
            // when & then
            assertThatThrownBy(() -> userService.getUserOrders(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
