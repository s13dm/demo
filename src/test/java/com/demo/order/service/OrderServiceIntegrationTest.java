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

import com.demo.common.exception.InsufficientStockException;
import com.demo.common.exception.OrderNotFoundException;
import com.demo.common.exception.ProductNotFoundException;
import com.demo.common.exception.UserNotFoundException;
import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.entity.DeliveryStatus;
import com.demo.product.dto.CreateProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.service.ProductService;
import com.demo.user.dto.CreateUserRequest;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.service.UserService;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    private Long userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        CreateUserResponse user = userService.registerUser(
                new CreateUserRequest("주문자", "order-test@example.com", "pass1234")
        );
        userId = user.userId();

        ProductResponse product = productService.addProduct(
                new CreateProductRequest("맥북 프로", 2500000, 100)
        );
        productId = product.productId();
    }

    @Nested
    @DisplayName("주문 생성 (placeOrder)")
    class PlaceOrderTest {

        @Test
        @DisplayName("정상적인 주문 생성 → 주문 정보 반환 및 재고 감소")
        void placeOrder_success() {
            CreateOrderRequest request = new CreateOrderRequest(
                    userId, productId, 1, "서울시 강남구 테헤란로 123"
            );

            CreateOrderResponse response = orderService.placeOrder(request);

            assertThat(response.orderId()).isNotNull();
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.productName()).isEqualTo("맥북 프로");
            assertThat(response.quantity()).isEqualTo(1);
            assertThat(response.shippingAddress()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.ORDERED);
            assertThat(response.orderedAt()).isNotNull();

            ProductResponse updatedProduct = productService.getProduct(productId);
            assertThat(updatedProduct.stock()).isEqualTo(99);
        }

        @Test
        @DisplayName("존재하지 않는 유저로 주문 → UserNotFoundException")
        void placeOrder_userNotFound_throwsException() {
            CreateOrderRequest request = new CreateOrderRequest(
                    999L, productId, 1, "서울시 강남구"
            );

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문 → ProductNotFoundException")
        void placeOrder_productNotFound_throwsException() {
            CreateOrderRequest request = new CreateOrderRequest(
                    userId, 999L, 1, "서울시 강남구"
            );

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("재고 부족 시 주문 → InsufficientStockException")
        void placeOrder_insufficientStock_throwsException() {
            CreateOrderRequest request = new CreateOrderRequest(
                    userId, productId, 999, "서울시 강남구"
            );

            assertThatThrownBy(() -> orderService.placeOrder(request))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    @DisplayName("배송 상태 조회 (checkDeliveryStatus)")
    class CheckDeliveryStatusTest {

        @Test
        @DisplayName("주문 후 배송 상태 조회 → ORDERED 상태 반환")
        void checkDeliveryStatus_success() {
            CreateOrderResponse order = orderService.placeOrder(
                    new CreateOrderRequest(userId, productId, 1, "서울시 서초구")
            );

            DeliveryStatusResponse response = orderService.checkDeliveryStatus(order.orderId());

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
            CreateOrderResponse order = orderService.placeOrder(
                    new CreateOrderRequest(userId, productId, 2, "서울시 마포구")
            );
            orderId = order.orderId();
        }

        @Test
        @DisplayName("ORDERED → PREPARING 상태 변경 → 성공")
        void updateDeliveryStatus_toPreparing_success() {
            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING);

            DeliveryStatusResponse response = orderService.updateDeliveryStatus(orderId, request);

            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
        }

        @Test
        @DisplayName("ORDERED → SHIPPED → DELIVERED 순차 변경 → 각 상태 정상 반영")
        void updateDeliveryStatus_fullFlow_success() {
            DeliveryStatusResponse step1 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING)
            );
            assertThat(step1.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);

            DeliveryStatusResponse step2 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.SHIPPED)
            );
            assertThat(step2.deliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);

            DeliveryStatusResponse step3 = orderService.updateDeliveryStatus(
                    orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.DELIVERED)
            );
            assertThat(step3.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID로 상태 변경 → OrderNotFoundException")
        void updateDeliveryStatus_orderNotFound_throwsException() {
            UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest(DeliveryStatus.SHIPPED);

            assertThatThrownBy(() -> orderService.updateDeliveryStatus(999L, request))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 (cancelOrder)")
    class CancelOrderTest {

        private Long orderId;

        @BeforeEach
        void setUp() {
            CreateOrderResponse order = orderService.placeOrder(
                    new CreateOrderRequest(userId, productId, 3, "서울시 종로구")
            );
            orderId = order.orderId();
        }

        @Test
        @DisplayName("ORDERED 상태 주문 취소 → CANCELLED 상태 + 재고 복구")
        void cancelOrder_success() {
            ProductResponse beforeCancel = productService.getProduct(productId);
            int stockBeforeCancel = beforeCancel.stock();

            DeliveryStatusResponse response = orderService.cancelOrder(orderId);

            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);

            ProductResponse afterCancel = productService.getProduct(productId);
            assertThat(afterCancel.stock()).isEqualTo(stockBeforeCancel + 3);
        }

        @Test
        @DisplayName("PREPARING 상태 주문 취소 → IllegalStateException")
        void cancelOrder_notOrderedStatus_throwsException() {
            orderService.updateDeliveryStatus(orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING));

            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID → OrderNotFoundException")
        void cancelOrder_orderNotFound_throwsException() {
            assertThatThrownBy(() -> orderService.cancelOrder(999L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }
}
