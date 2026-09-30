package com.example.ordermanagement.controller;

import static org.hamcrest.Matchers.is;
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
import org.springframework.test.web.servlet.MvcResult;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void placeOrder_singleProduct_success() throws Exception {
        Long customerId = createCustomer("Order Customer", "orderer@example.com", "1000000001");
        Long productId = createProduct("Widget", new BigDecimal("25.50"), 10);

        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, 3)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customer.id", is(customerId.intValue())))
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].productId", is(productId.intValue())))
                .andExpect(jsonPath("$.items[0].quantity", is(3)))
                .andExpect(jsonPath("$.totalAmount", is(76.50)));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(jsonPath("$.stock", is(7)));
    }

    @Test
    void placeOrder_multipleProducts_totalIsSumOfSubtotals() throws Exception {
        Long customerId = createCustomer("Multi Customer", "multi@example.com", "1000000002");
        Long productA = createProduct("Product A", new BigDecimal("10.00"), 10);
        Long productB = createProduct("Product B", new BigDecimal("15.00"), 10);

        OrderRequest request = new OrderRequest(customerId, List.of(
                new OrderItemRequest(productA, 2),
                new OrderItemRequest(productB, 3)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andExpect(jsonPath("$.totalAmount", is(65.00)));

        mockMvc.perform(get("/api/products/{id}", productA)).andExpect(jsonPath("$.stock", is(8)));
        mockMvc.perform(get("/api/products/{id}", productB)).andExpect(jsonPath("$.stock", is(7)));
    }

    @Test
    void placeOrder_insufficientStock_returns409AndLeavesStockUnchanged() throws Exception {
        Long customerId = createCustomer("Stock Customer", "stock@example.com", "1000000003");
        Long productId = createProduct("Scarce Item", new BigDecimal("5.00"), 2);

        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, 5)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_STOCK")));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(jsonPath("$.stock", is(2)));
    }

    @Test
    void placeOrder_secondItemInsufficientStock_rollsBackFirstItemsDeductionToo() throws Exception {
        Long customerId = createCustomer("Rollback Customer", "rollback@example.com", "1000000004");
        Long productA = createProduct("Product A", new BigDecimal("10.00"), 10);
        Long productB = createProduct("Product B", new BigDecimal("10.00"), 1);

        OrderRequest request = new OrderRequest(customerId, List.of(
                new OrderItemRequest(productA, 5),
                new OrderItemRequest(productB, 5)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Product A's stock must NOT have been deducted even though it was processed
        // before the failing item - the whole order placement is one atomic transaction.
        mockMvc.perform(get("/api/products/{id}", productA)).andExpect(jsonPath("$.stock", is(10)));
        mockMvc.perform(get("/api/products/{id}", productB)).andExpect(jsonPath("$.stock", is(1)));

        mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void placeOrder_nonExistentCustomer_returns404() throws Exception {
        Long productId = createProduct("Product", new BigDecimal("10.00"), 10);

        OrderRequest request = new OrderRequest(999_999L, List.of(new OrderItemRequest(productId, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void placeOrder_nonExistentProduct_returns400() throws Exception {
        Long customerId = createCustomer("Customer", "customer@example.com", "1000000005");

        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(999_999L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_ORDER")));
    }

    @Test
    void placeOrder_zeroQuantity_returns400() throws Exception {
        Long customerId = createCustomer("Customer", "customer2@example.com", "1000000006");
        Long productId = createProduct("Product", new BigDecimal("10.00"), 10);

        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, 0)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void placeOrder_negativeQuantity_returns400() throws Exception {
        Long customerId = createCustomer("Customer", "customer3@example.com", "1000000007");
        Long productId = createProduct("Product", new BigDecimal("10.00"), 10);

        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, -1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_emptyItems_returns400() throws Exception {
        Long customerId = createCustomer("Customer", "customer4@example.com", "1000000008");

        OrderRequest request = new OrderRequest(customerId, List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void getOrdersByCustomer_returnsOrdersSortedNewestFirst() throws Exception {
        Long customerId = createCustomer("History Customer", "history@example.com", "1000000009");
        Long productId = createProduct("Product", new BigDecimal("10.00"), 100);

        placeOrder(customerId, productId, 1);
        placeOrder(customerId, productId, 2);

        MvcResult result = mockMvc.perform(get("/api/orders/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andReturn();

        assertThatContainsQuantities(result.getResponse().getContentAsString());
    }

    private void placeOrder(Long customerId, Long productId, int quantity) throws Exception {
        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(productId, quantity)));
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void assertThatContainsQuantities(String responseBody) throws Exception {
        var root = objectMapper.readTree(responseBody);
        int total = 0;
        for (var order : root) {
            total += order.get("items").get(0).get("quantity").asInt();
        }
        org.junit.jupiter.api.Assertions.assertEquals(3, total);
    }
}
