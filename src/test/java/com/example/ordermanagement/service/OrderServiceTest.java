package com.example.ordermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.exception.InsufficientStockException;
import com.example.ordermanagement.exception.InvalidOrderException;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;

    private Customer aCustomer() {
        return Customer.builder().id(1L).name("Jane").email("jane@example.com").phone("1234567890").build();
    }

    @Test
    void placeOrder_singleProduct_success() {
        customer = aCustomer();
        Product product = Product.builder().id(10L).name("Widget").price(new BigDecimal("2.50")).stock(10).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByIdInForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(10L, 3)));
        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("7.50");
        assertThat(response.getItems()).hasSize(1);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    void placeOrder_multipleProducts_totalIsSumOfSubtotals() {
        customer = aCustomer();
        Product productA = Product.builder().id(10L).name("A").price(new BigDecimal("10.00")).stock(10).build();
        Product productB = Product.builder().id(20L).name("B").price(new BigDecimal("5.00")).stock(10).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByIdInForUpdate(List.of(10L, 20L))).thenReturn(List.of(productA, productB));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = new OrderRequest(1L, List.of(
                new OrderItemRequest(10L, 2),
                new OrderItemRequest(20L, 4)));
        OrderResponse response = orderService.placeOrder(request);

        // 2*10.00 + 4*5.00 = 40.00
        assertThat(response.getTotalAmount()).isEqualByComparingTo("40.00");
        assertThat(productA.getStock()).isEqualTo(8);
        assertThat(productB.getStock()).isEqualTo(6);
    }

    @Test
    void placeOrder_insufficientStock_throwsAndDoesNotPersistOrder() {
        customer = aCustomer();
        Product product = Product.builder().id(10L).name("Widget").price(new BigDecimal("2.50")).stock(2).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByIdInForUpdate(List.of(10L))).thenReturn(List.of(product));

        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(10L, 5)));

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));
        assertThat(product.getStock()).isEqualTo(2);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_nonExistentCustomer_throwsResourceNotFoundException() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(10L, 1)));

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(request));
        verify(productRepository, never()).findAllByIdInForUpdate(anyList());
    }

    @Test
    void placeOrder_nonExistentProduct_throwsInvalidOrderException() {
        customer = aCustomer();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByIdInForUpdate(List.of(10L))).thenReturn(List.of());

        OrderRequest request = new OrderRequest(1L, List.of(new OrderItemRequest(10L, 1)));

        assertThrows(InvalidOrderException.class, () -> orderService.placeOrder(request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByCustomer_nonExistentCustomer_throwsResourceNotFoundException() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrdersByCustomer(1L));
    }
}
