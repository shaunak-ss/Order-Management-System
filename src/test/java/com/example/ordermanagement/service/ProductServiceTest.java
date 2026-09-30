package com.example.ordermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.dto.response.ProductResponse;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_success_returnsMappedResponse() {
        ProductRequest request = new ProductRequest("Laptop", new BigDecimal("999.99"), 5);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getPrice()).isEqualByComparingTo("999.99");
        assertThat(response.getStock()).isEqualTo(5);
    }

    @Test
    void updateProduct_success_updatesFields() {
        Product existing = Product.builder().id(1L).name("Old").price(new BigDecimal("10.00")).stock(1).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("New", new BigDecimal("20.00"), 10);
        ProductResponse response = productService.updateProduct(1L, request);

        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getPrice()).isEqualByComparingTo("20.00");
        assertThat(response.getStock()).isEqualTo(10);
    }

    @Test
    void updateProduct_nonExistent_throwsResourceNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        ProductRequest request = new ProductRequest("Name", new BigDecimal("1.00"), 1);

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(999L, request));
    }

    @Test
    void getProduct_nonExistent_throwsResourceNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(999L));
    }
}
