package com.example.ordermanagement.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanagement.dto.request.ProductRequest;
import com.example.ordermanagement.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createProduct_returns201WithBody() throws Exception {
        ProductRequest request = new ProductRequest("Laptop", new BigDecimal("50000.00"), 10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.price", is(50000.00)))
                .andExpect(jsonPath("$.stock", is(10)));
    }

    @Test
    void createProduct_missingName_returns400() throws Exception {
        ProductRequest request = new ProductRequest("", new BigDecimal("10.00"), 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_negativePrice_returns400() throws Exception {
        ProductRequest request = new ProductRequest("Bad Product", new BigDecimal("-1.00"), 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void createProduct_negativeStock_returns400() throws Exception {
        ProductRequest request = new ProductRequest("Bad Product", new BigDecimal("10.00"), -5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void getProduct_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_success_returns200WithUpdatedFields() throws Exception {
        Long id = createProduct("Old Product", new BigDecimal("100.00"), 5);

        ProductRequest update = new ProductRequest("New Product", new BigDecimal("150.00"), 20);

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Product")))
                .andExpect(jsonPath("$.price", is(150.00)))
                .andExpect(jsonPath("$.stock", is(20)));
    }

    @Test
    void updateProduct_nonExistent_returns404() throws Exception {
        ProductRequest update = new ProductRequest("Name", new BigDecimal("10.00"), 1);

        mockMvc.perform(put("/api/products/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listProducts_returnsAllCreatedProducts() throws Exception {
        createProduct("P1", new BigDecimal("10.00"), 1);
        createProduct("P2", new BigDecimal("20.00"), 2);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }
}
