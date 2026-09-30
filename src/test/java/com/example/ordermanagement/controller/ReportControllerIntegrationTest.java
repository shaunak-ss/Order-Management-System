package com.example.ordermanagement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanagement.dto.request.OrderItemRequest;
import com.example.ordermanagement.dto.request.OrderRequest;
import com.example.ordermanagement.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ReportControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void ordersPerCustomer_includesCustomersWithZeroOrders() throws Exception {
        Long withOrders = createCustomer("Has Orders", "hasorders@example.com", "2000000001");
        createCustomer("No Orders", "noorders@example.com", "2000000002");
        Long productId = createProduct("Product", new BigDecimal("10.00"), 100);

        placeOrder(withOrders, productId, 1);

        mockMvc.perform(get("/api/reports/orders-per-customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.is(2)))
                .andExpect(jsonPath("$[?(@.customerId == " + withOrders + ")].orderCount", org.hamcrest.Matchers.contains(1)));
    }

    @Test
    void topCustomers_ordersByCountDescendingAndLimitsToFive() throws Exception {
        Long productId = createProduct("Product", new BigDecimal("10.00"), 1000);

        // 6 customers with distinct order counts: 5, 4, 3, 2, 1, 0
        Long c5 = createCustomer("Five Orders", "five@example.com", "3000000001");
        Long c4 = createCustomer("Four Orders", "four@example.com", "3000000002");
        Long c3 = createCustomer("Three Orders", "three@example.com", "3000000003");
        Long c2 = createCustomer("Two Orders", "two@example.com", "3000000004");
        Long c1 = createCustomer("One Order", "one@example.com", "3000000005");
        createCustomer("Zero Orders", "zero@example.com", "3000000006");

        placeOrders(c5, productId, 5);
        placeOrders(c4, productId, 4);
        placeOrders(c3, productId, 3);
        placeOrders(c2, productId, 2);
        placeOrders(c1, productId, 1);

        mockMvc.perform(get("/api/reports/top-customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.is(5)))
                .andExpect(jsonPath("$[0].customerId", org.hamcrest.Matchers.is(c5.intValue())))
                .andExpect(jsonPath("$[0].orderCount", org.hamcrest.Matchers.is(5)))
                .andExpect(jsonPath("$[4].customerId", org.hamcrest.Matchers.is(c1.intValue())))
                .andExpect(jsonPath("$[4].orderCount", org.hamcrest.Matchers.is(1)));
    }

    @Test
    void topCustomers_tiedCountsBrokenByCustomerIdAscending() throws Exception {
        Long productId = createProduct("Product", new BigDecimal("10.00"), 100);

        Long first = createCustomer("First", "first@example.com", "4000000001");
        Long second = createCustomer("Second", "second@example.com", "4000000002");

        placeOrders(first, productId, 2);
        placeOrders(second, productId, 2);

        mockMvc.perform(get("/api/reports/top-customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.is(2)))
                .andExpect(jsonPath("$[0].customerId", org.hamcrest.Matchers.is(first.intValue())))
                .andExpect(jsonPath("$[1].customerId", org.hamcrest.Matchers.is(second.intValue())));
    }

    @Test
    void topCustomers_fewerThanFiveCustomers_returnsAllOfThem() throws Exception {
        Long productId = createProduct("Product", new BigDecimal("10.00"), 100);
        Long only = createCustomer("Only Customer", "only@example.com", "5000000001");

        placeOrders(only, productId, 1);

        mockMvc.perform(get("/api/reports/top-customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.is(1)));
    }

    private void placeOrders(Long customerId, Long productId, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            placeOrder(customerId, productId, 1);
        }
    }

    private void placeOrder(Long customerId, Long productId, int quantity) throws Exception {
        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, quantity)));
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
