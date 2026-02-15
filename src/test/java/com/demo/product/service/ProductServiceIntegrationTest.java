package com.demo.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.DuplicateProductNameException;
import com.demo.common.exception.ProductNotFoundException;
import com.demo.product.dto.CreateProductRequest;
import com.demo.product.dto.ProductResponse;

@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Nested
    @DisplayName("상품 추가 (addProduct)")
    class AddProductTest {

        @Test
        @DisplayName("정상적인 상품 추가 → 상품 정보 반환")
        void addProduct_success() {
            CreateProductRequest request = new CreateProductRequest("맥북 프로", 2500000, 50);

            ProductResponse response = productService.addProduct(request);

            assertThat(response.productId()).isNotNull();
            assertThat(response.name()).isEqualTo("맥북 프로");
            assertThat(response.price()).isEqualTo(2500000);
            assertThat(response.stock()).isEqualTo(50);
        }

        @Test
        @DisplayName("중복 상품명 → DuplicateProductNameException")
        void addProduct_duplicateName_throwsException() {
            productService.addProduct(new CreateProductRequest("맥북 프로", 2500000, 50));

            assertThatThrownBy(() ->
                    productService.addProduct(new CreateProductRequest("맥북 프로", 3000000, 30))
            ).isInstanceOf(DuplicateProductNameException.class);
        }
    }

    @Nested
    @DisplayName("상품 조회 (getProduct)")
    class GetProductTest {

        @Test
        @DisplayName("존재하는 상품 조회 → 상품 정보 반환")
        void getProduct_success() {
            ProductResponse created = productService.addProduct(
                    new CreateProductRequest("키보드", 150000, 100)
            );

            ProductResponse response = productService.getProduct(created.productId());

            assertThat(response.productId()).isEqualTo(created.productId());
            assertThat(response.name()).isEqualTo("키보드");
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 → ProductNotFoundException")
        void getProduct_notFound_throwsException() {
            assertThatThrownBy(() -> productService.getProduct(999L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("전체 상품 조회 (getAllProducts)")
    class GetAllProductsTest {

        @Test
        @DisplayName("상품 목록 조회 → 등록된 상품 리스트 반환")
        void getAllProducts_success() {
            productService.addProduct(new CreateProductRequest("노트북", 1500000, 30));
            productService.addProduct(new CreateProductRequest("마우스", 50000, 200));

            List<ProductResponse> products = productService.getAllProducts();

            assertThat(products).hasSizeGreaterThanOrEqualTo(2);
        }
    }
}
