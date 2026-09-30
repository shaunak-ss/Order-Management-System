package com.example.ordermanagement.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Proves the pessimistic-locking concurrency strategy documented on
 * {@link com.example.ordermanagement.service.OrderService}: concurrent order placements
 * for the same product can never over-sell stock, i.e. stock can never go negative and
 * the sum of successful deductions must exactly match what was actually available.
 */
class OrderConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Test
    void twoConcurrentOrdersExceedingStock_onlyOneSucceeds() throws Exception {
        Long customerA = createCustomer("Customer A", "concurrentA@example.com", "9000000001");
        Long customerB = createCustomer("Customer B", "concurrentB@example.com", "9000000002");
        Long productId = createProduct("Contended Product", new BigDecimal("10.00"), 10);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> orderA = placeOrderTask(startLatch, customerA, productId, 7);
        Callable<Integer> orderB = placeOrderTask(startLatch, customerB, productId, 7);

        Future<Integer> futureA = executor.submit(orderA);
        Future<Integer> futureB = executor.submit(orderB);

        startLatch.countDown();

        int statusA = futureA.get(10, TimeUnit.SECONDS);
        int statusB = futureB.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<Integer> statuses = List.of(statusA, statusB);
        long successCount = statuses.stream().filter(s -> s == 201).count();
        long conflictCount = statuses.stream().filter(s -> s == 409).count();

        assertThat(successCount).as("exactly one of the two overlapping orders should succeed").isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.stock", org.hamcrest.Matchers.is(3)));
    }

    @Test
    void manyConcurrentOrders_neverOversellStock() throws Exception {
        int initialStock = 20;
        int concurrentRequests = 20;
        Long productId = createProduct("Hot Product", new BigDecimal("5.00"), initialStock);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger customerCounter = new AtomicInteger();

        List<Callable<Integer>> tasks = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> (Callable<Integer>) () -> {
                    Long customerId = createCustomer(
                            "Bulk Customer " + i,
                            "bulk" + i + "@example.com",
                            "800000" + String.format("%04d", customerCounter.incrementAndGet()));
                    startLatch.await();
                    return placeOrder(customerId, productId, 1);
                })
                .toList();

        List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
        startLatch.countDown();

        List<Integer> statuses = futures.stream()
                .map(f -> {
                    try {
                        return f.get(15, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        executor.shutdown();

        long successCount = statuses.stream().filter(s -> s == 201).count();
        assertThat(successCount).isEqualTo(initialStock);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.stock", org.hamcrest.Matchers.is(0)));
    }

    private Callable<Integer> placeOrderTask(CountDownLatch startLatch, Long customerId, Long productId, int quantity) {
        return () -> {
            startLatch.await();
            return placeOrder(customerId, productId, quantity);
        };
    }

    private int placeOrder(Long customerId, Long productId, int quantity) throws Exception {
        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, quantity)));
        return mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getStatus();
    }
}
