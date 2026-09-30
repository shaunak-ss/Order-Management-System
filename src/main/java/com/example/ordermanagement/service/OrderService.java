package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.dto.response.OrderResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.entity.Order;
import com.example.ordermanagement.entity.OrderItem;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.exception.InsufficientStockException;
import com.example.ordermanagement.exception.InvalidOrderException;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.mapper.OrderMapper;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.repository.OrderRepository;
import com.example.ordermanagement.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order placement is the most business-critical workflow in the system: it must be
 * atomic (all-or-nothing) and must remain correct when multiple customers concurrently
 * order the same product.
 *
 * <p><b>Concurrency strategy:</b> when placing an order, the products involved are
 * loaded with a pessimistic write lock ({@code SELECT ... FOR UPDATE} via
 * {@link ProductRepository#findAllByIdInForUpdate}) inside the surrounding
 * {@code @Transactional} boundary. This means two concurrent transactions that both try
 * to order the same product will serialize: the second transaction blocks at the SELECT
 * until the first commits (releasing the lock) or rolls back, and then re-reads the
 * now-current stock. This makes the classic "stock=5, two requests both read 5, both
 * succeed, stock goes negative" race impossible. Row locks are always acquired in
 * ascending product-id order, so two orders that both touch products {1, 2} can never
 * deadlock by locking them in opposite order.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for customerId={}, itemCount={}", request.getCustomerId(), request.getItems().size());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Customer", request.getCustomerId()));

        Map<Long, Product> lockedProductsById = lockProductsInDeterministicOrder(request.getItems());

        Order order = Order.builder().customer(customer).build();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = lockedProductsById.get(itemRequest.getProductId());
            int quantity = itemRequest.getQuantity();

            if (!product.hasSufficientStock(quantity)) {
                log.warn("Insufficient stock for productId={}: requested={}, available={}",
                        product.getId(), quantity, product.getStock());
                throw new InsufficientStockException(
                        "Insufficient stock for product '%s' (id=%d): requested %d, available %d"
                                .formatted(product.getName(), product.getId(), quantity, product.getStock()));
            }

            product.deductStock(quantity);

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            order.addItem(OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        log.info("Order placed successfully: orderId={}, customerId={}, totalAmount={}",
                savedOrder.getId(), customer.getId(), totalAmount);

        return OrderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw ResourceNotFoundException.forEntity("Customer", customerId);
        }
        return orderRepository.findByCustomerIdWithDetails(customerId).stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    /**
     * Locks every distinct product referenced by the order, in ascending id order, and
     * verifies each one exists. Locking in a deterministic order (rather than the order
     * items happen to appear in the request) is what prevents deadlocks when two orders
     * reference an overlapping set of products.
     */
    private Map<Long, Product> lockProductsInDeterministicOrder(List<OrderItemRequest> items) {
        List<Long> distinctSortedProductIds = items.stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Product> lockedProducts = productRepository.findAllByIdInForUpdate(distinctSortedProductIds);
        Map<Long, Product> productsById = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> missingProductIds = distinctSortedProductIds.stream()
                .filter(id -> !productsById.containsKey(id))
                .toList();
        if (!missingProductIds.isEmpty()) {
            throw new InvalidOrderException("Product(s) not found: " + missingProductIds);
        }

        return productsById;
    }
}
