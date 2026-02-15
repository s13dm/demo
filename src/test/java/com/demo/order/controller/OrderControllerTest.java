package com.demo.order.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.demo.order.dto.CreateOrderResponse;
import com.demo.order.dto.DeliveryStatusResponse;
import com.demo.order.entity.DeliveryStatus;
import com.demo.order.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrder_returnsCreatedOrder() throws Exception {
        when(orderService.placeOrder(org.mockito.ArgumentMatchers.any()))
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

    @Test
    void checkDeliveryStatus_returnsCurrentState() throws Exception {
        when(orderService.checkDeliveryStatus(10L))
                .thenReturn(new DeliveryStatusResponse(10L, DeliveryStatus.SHIPPED, LocalDateTime.parse("2026-01-01T10:00:00")));

        mockMvc.perform(get("/api/orders/10/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPED"));
    }
}
