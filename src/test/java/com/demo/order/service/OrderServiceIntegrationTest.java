package com.demo.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.OrderNotFoundException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.entity.DeliveryStatus;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.service.UserService;

/*
 * ================================================
 * OrderService 통합 테스트
 * ================================================
 *
 * [@SpringBootTest vs @WebMvcTest vs @DataJpaTest 차이점]
 *
 * @WebMvcTest:    Controller 레이어만 테스트. Service는 Mock 처리.
 * @DataJpaTest:   Repository 레이어만 테스트. 임베디드 DB 사용. Service는 로드 안 됨.
 * @SpringBootTest: 전체 애플리케이션 컨텍스트 로드. Service + Repository + DB 전부 실제로 동작.
 *                  → 이 테스트에서 사용하는 방식.
 *
 * [왜 @SpringBootTest를 쓰는가?]
 * - Service 레이어의 비즈니스 로직이 실제 DB와 함께 올바르게 동작하는지 검증.
 * - Mock 없이 실제 트랜잭션, JPA 쿼리, 엔티티 매핑까지 전부 테스트한다.
 */
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    // 테스트에서 공통으로 사용할 유저 ID (BeforeEach에서 등록됨)
    private Long userId;

    /*
     * @BeforeEach: 클래스 레벨에서 선언하면 모든 @Test 전에 실행된다.
     * @Nested 안에서도 상위 클래스의 @BeforeEach가 먼저 실행된 후
     * @Nested의 @BeforeEach가 실행된다 (부모 → 자식 순서).
     */
    @BeforeEach
    void setUp() {
        // 주문에는 반드시 유저가 필요하므로 유저를 미리 등록
        CreateUserResponse user = userService.registerUser(
                new CreateUserRequest("주문자", "order-test@example.com", "pass1234")
        );
        userId = user.userId();
    }

    @Nested
    @DisplayName("주문 생성 (placeOrder)")
    class PlaceOrderTest {

        @Test
        @DisplayName("정상적인 주문 생성 → 주문 정보 반환")
        void placeOrder_success() {
            // given
            CreateOrderRequest request = new CreateOrderRequest(
                    userId, "맥북 프로", 1, "서울시 강남구 테헤란로 123"
            );

            // when
            CreateOrderResponse response = orderService.placeOrder(request);

            // then
            assertThat(response.orderId()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.productName()).isEqualTo("맥북 프로");
            assertThat(response.quantity()).isEqualTo(1);
            assertThat(response.shippingAddress()).isEqualTo("서울시 강남구 테헤란로 123");

            /*
             * DeliveryStatus.ORDERED: 주문 생성 시 Order 엔티티 생성자에서
             * 기본값으로 ORDERED가 설정된다 (Order.java 참고).
             */
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.ORDERED);

            /*
             * .isNotNull(): orderedAt이 null이 아닌지 확인.
             * Order 생성자에서 LocalDateTime.now()로 자동 설정되므로 항상 값이 있어야 한다.
             */
            assertThat(response.orderedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 유저로 주문 → UserNotFoundException")
        void placeOrder_userNotFound_throwsException() {
            // given: 존재하지 않는 유저 ID
            CreateOrderRequest request = new CreateOrderRequest(
                    999L, "맥북 프로", 1, "서울시 강남구"
            );

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("배송 상태 조회 (checkDeliveryStatus)")
    class CheckDeliveryStatusTest {

        @Test
        @DisplayName("주문 후 배송 상태 조회 → ORDERED 상태 반환")
        void checkDeliveryStatus_success() {
            // given: 주문 생성
            CreateOrderResponse order = orderService.placeOrder(
                    new CreateOrderRequest(userId, "키보드", 1, "서울시 서초구")
            );

            // when
            /*
             * @Transactional(readOnly = true)가 서비스에 붙어있는 메서드.
             * 읽기 전용 트랜잭션은 JPA가 변경 감지(dirty checking)를 수행하지 않아
             * 성능상 이점이 있다.
             */
            DeliveryStatusResponse response = orderService.checkDeliveryStatus(order.orderId());

            // then
            assertThat(response.orderId()).isEqualTo(order.orderId());
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.ORDERED);
            assertThat(response.orderedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID → OrderNotFoundException")
        void checkDeliveryStatus_orderNotFound_throwsException() {
            assertThatThrownBy(() -> orderService.checkDeliveryStatus(999L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("배송 상태 변경 (updateDeliveryStatus)")
    class UpdateDeliveryStatusTest {

        private Long orderId;

        @BeforeEach
        void setUp() {
            // 배송 상태 변경 테스트를 위해 주문을 미리 생성
            CreateOrderResponse order = orderService.placeOrder(
                    new CreateOrderRequest(userId, "모니터", 2, "서울시 마포구")
            );
            orderId = order.orderId();
        }

        @Test
        @DisplayName("ORDERED → PREPARING 상태 변경 → 성공")
        void updateDeliveryStatus_toPreparing_success() {
            // given
            /*
             * record 생성 방법:
             * UpdateDeliveryStatusRequest는 Java record이므로
             * new UpdateDeliveryStatusRequest(값)으로 생성한다.
             * record는 생성자, getter, equals, hashCode, toString을 자동 생성한다.
             */
            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING);

            // when
            DeliveryStatusResponse response = orderService.updateDeliveryStatus(orderId, request);

            // then
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
        }

        @Test
        @DisplayName("ORDERED → SHIPPED → DELIVERED 순차 변경 → 각 상태 정상 반영")
        void updateDeliveryStatus_fullFlow_success() {
            // given & when & then: 배송 전체 흐름 테스트

            // 1단계: ORDERED → PREPARING
            DeliveryStatusResponse step1 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING)
            );
            assertThat(step1.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);

            // 2단계: PREPARING → SHIPPED
            DeliveryStatusResponse step2 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.SHIPPED)
            );
            assertThat(step2.deliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);

            // 3단계: SHIPPED → DELIVERED
            DeliveryStatusResponse step3 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.DELIVERED)
            );
            assertThat(step3.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);

            /*
             * [변경 감지 (Dirty Checking)]
             * order.changeDeliveryStatus()로 엔티티의 필드를 변경하면,
             * JPA가 트랜잭션 커밋 시점에 자동으로 UPDATE SQL을 발행한다.
             * → save()를 명시적으로 호출하지 않아도 DB에 반영된다.
             * 이것이 JPA의 "변경 감지" 메커니즘이다.
             */
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID로 상태 변경 → OrderNotFoundException")
        void updateDeliveryStatus_orderNotFound_throwsException() {
            // given
            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(DeliveryStatus.SHIPPED);

            // when & then
            assertThatThrownBy(() -> orderService.updateDeliveryStatus(999L, request))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }
}
