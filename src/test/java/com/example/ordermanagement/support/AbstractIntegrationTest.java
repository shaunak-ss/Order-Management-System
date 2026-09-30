package com.example.ordermanagement.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.ProductRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for full-stack (real H2 database via Flyway, real HTTP layer via MockMvc)
 * integration tests. The database is wiped before every test method so that reporting
 * assertions (which aggregate across all customers/orders) are deterministic regardless
 * of test execution order or what other test classes have written to the shared,
 * context-cached database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM customers");
    }

    protected Long createCustomer(String name, String email, String phone) throws Exception {
        CustomerRequest request = new CustomerRequest(name, email, phone);
        String body = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractId(body);
    }

    protected Long createProduct(String name, BigDecimal price, int stock) throws Exception {
        ProductRequest request = new ProductRequest(name, price, stock);
        String body = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractId(body);
    }

    private Long extractId(String responseBody) throws Exception {
        JsonNode node = objectMapper.readTree(responseBody);
        return node.get("id").asLong();
    }
}
