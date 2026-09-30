package com.example.ordermanagement.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CustomerControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createCustomer_returns201WithLocationAndBody() throws Exception {
        CustomerRequest request = new CustomerRequest("John Doe", "john@example.com", "9999999999");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.phone", is("9999999999")));
    }

    @Test
    void createCustomer_missingName_returns400() throws Exception {
        CustomerRequest request = new CustomerRequest("", "jane@example.com", "8888888888");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void createCustomer_invalidEmail_returns400() throws Exception {
        CustomerRequest request = new CustomerRequest("Jane Doe", "not-an-email", "8888888888");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void createCustomer_missingPhone_returns400() throws Exception {
        CustomerRequest request = new CustomerRequest("Jane Doe", "jane@example.com", "");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCustomer_duplicateEmail_returns409() throws Exception {
        createCustomer("First Customer", "dup@example.com", "1111111111");

        CustomerRequest duplicate = new CustomerRequest("Second Customer", "dup@example.com", "2222222222");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }

    @Test
    void createCustomer_duplicatePhone_returns409() throws Exception {
        createCustomer("First Customer", "first@example.com", "3333333333");

        CustomerRequest duplicate = new CustomerRequest("Second Customer", "second@example.com", "3333333333");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_RESOURCE")));
    }

    @Test
    void getCustomer_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void getCustomer_existing_returns200() throws Exception {
        Long id = createCustomer("Fetch Me", "fetchme@example.com", "4444444444");

        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.name", is("Fetch Me")));
    }

    @Test
    void updateCustomer_success_returns200WithUpdatedFields() throws Exception {
        Long id = createCustomer("Old Name", "old@example.com", "5555555555");

        CustomerRequest update = new CustomerRequest("New Name", "new@example.com", "6666666666");

        mockMvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                .andExpect(jsonPath("$.email", is("new@example.com")))
                .andExpect(jsonPath("$.phone", is("6666666666")));
    }

    @Test
    void updateCustomer_nonExistent_returns404() throws Exception {
        CustomerRequest update = new CustomerRequest("Name", "someone@example.com", "7777777777");

        mockMvc.perform(put("/api/customers/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCustomers_returnsAllCreatedCustomers() throws Exception {
        createCustomer("A", "a@example.com", "1010101010");
        createCustomer("B", "b@example.com", "2020202020");

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }
}
