package com.demo.product.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.demo.product.dto.ProductResponse;
import com.demo.product.service.ProductService;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void addProduct_returnsCreatedProduct() throws Exception {
        when(productService.addProduct(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductResponse(1L, "맥북 프로", 2500000, 50));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "맥북 프로",
                                  "price": 2500000,
                                  "stock": 50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("맥북 프로"))
                .andExpect(jsonPath("$.stock").value(50));
    }

    @Test
    void getProduct_returnsProduct() throws Exception {
        when(productService.getProduct(1L))
                .thenReturn(new ProductResponse(1L, "맥북 프로", 2500000, 50));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("맥북 프로"));
    }

    @Test
    void getAllProducts_returnsProductList() throws Exception {
        when(productService.getAllProducts())
                .thenReturn(List.of(
                        new ProductResponse(1L, "맥북 프로", 2500000, 50),
                        new ProductResponse(2L, "키보드", 150000, 100)
                ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("맥북 프로"))
                .andExpect(jsonPath("$[1].name").value("키보드"));
    }
}
