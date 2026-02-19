package com.demo.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.demo.order.dto.CreateOrderRequest;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.dto.UpdateDeliveryStatusRequest;
import com.demo.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다. 재고가 충분해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "재고 부족")
    })
    public CreateOrderResponse placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/{orderId}/delivery")
    @Operation(summary = "배송 상태 조회", description = "특정 주문의 현재 배송 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    public DeliveryStatusResponse checkDeliveryStatus(
            @Parameter(description = "조회할 주문 ID", required = true) @PathVariable Long orderId) {
        return orderService.checkDeliveryStatus(orderId);
    }

    @PatchMapping("/{orderId}/delivery")
    @Operation(summary = "배송 상태 업데이트", description = "특정 주문의 배송 상태를 업데이트합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 배송 상태"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    public DeliveryStatusResponse updateDeliveryStatus(
            @Parameter(description = "업데이트할 주문 ID", required = true) @PathVariable Long orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request
    ) {
        return orderService.updateDeliveryStatus(orderId, request);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "특정 주문을 취소합니다. 이미 배송된 주문은 취소할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소할 수 없는 주문 상태"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    public DeliveryStatusResponse cancelOrder(
            @Parameter(description = "취소할 주문 ID", required = true) @PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }
}
