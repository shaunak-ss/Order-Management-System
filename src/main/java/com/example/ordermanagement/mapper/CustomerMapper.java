package com.example.ordermanagement.mapper;

import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
