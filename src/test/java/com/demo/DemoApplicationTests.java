package com.demo;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.demo.order.controller.OrderDeliveryController;
import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.CreateUserResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.entity.DeliveryStatus;
import com.demo.order.service.OrderDeliveryService;

@WebMvcTest(OrderDeliveryController.class)
class DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderDeliveryService orderDeliveryService;

    @Test
    void registerUser_returnsCreatedUser() throws Exception {
        when(orderDeliveryService.registerUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CreateUserResponse(1L, "홍길동", "hong@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "email": "hong@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("hong@example.com"));
    }

    @Test
    void checkDeliveryStatus_returnsCurrentState() throws Exception {
        when(orderDeliveryService.checkDeliveryStatus(10L))
                .thenReturn(new DeliveryStatusResponse(10L, DeliveryStatus.SHIPPED, LocalDateTime.parse("2026-01-01T10:00:00")));

        mockMvc.perform(get("/api/orders/10/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPED"));
    }

    @Test
    void placeOrder_returnsCreatedOrder() throws Exception {
        when(orderDeliveryService.placeOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CreateOrderResponse(
                        22L,
                        1L,
                        "노트북",
                        1,
                        "서울시 강남구",
                        DeliveryStatus.ORDERED,
                        LocalDateTime.parse("2026-01-01T09:00:00")
                ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productName": "노트북",
                                  "quantity": 1,
                                  "shippingAddress": "서울시 강남구"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(22))
                .andExpect(jsonPath("$.deliveryStatus").value("ORDERED"));
    }
}
