package com.demo.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.common.exception.DuplicateProductNameException;
import com.demo.common.exception.ProductNotFoundException;
import com.demo.product.dto.CreateProductRequest;
import com.demo.product.dto.ProductResponse;
import com.demo.product.entity.Product;
import com.demo.product.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse addProduct(CreateProductRequest request) {
        if (productRepository.existsByName(request.name())) {
            throw new DuplicateProductNameException(request.name());
        }

        Product product = productRepository.save(
                new Product(request.name(), request.price(), request.stock())
        );

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}
