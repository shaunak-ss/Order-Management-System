package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.response.CustomerOrderCountResponse;
import com.example.ordermanagement.repository.CustomerOrderCountProjection;
import com.example.ordermanagement.repository.OrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int TOP_CUSTOMERS_LIMIT = 5;

    private final OrderRepository orderRepository;

    public List<CustomerOrderCountResponse> getOrdersPerCustomer() {
        return orderRepository.countOrdersPerCustomer().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CustomerOrderCountResponse> getTopCustomers() {
        return orderRepository.findTopCustomersByOrderCount(PageRequest.of(0, TOP_CUSTOMERS_LIMIT)).stream()
                .map(this::toResponse)
                .toList();
    }

    private CustomerOrderCountResponse toResponse(CustomerOrderCountProjection projection) {
        return CustomerOrderCountResponse.builder()
                .customerId(projection.getCustomerId())
                .customerName(projection.getCustomerName())
                .orderCount(projection.getOrderCount())
                .build();
    }
}
