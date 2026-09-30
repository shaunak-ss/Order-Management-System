package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.response.CustomerOrderCountResponse;
import com.example.ordermanagement.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Order reporting/aggregation")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/orders-per-customer")
    @Operation(summary = "Total orders placed by each customer")
    public ResponseEntity<List<CustomerOrderCountResponse>> getOrdersPerCustomer() {
        return ResponseEntity.ok(reportService.getOrdersPerCustomer());
    }

    @GetMapping("/top-customers")
    @Operation(summary = "Top 5 customers by number of orders")
    public ResponseEntity<List<CustomerOrderCountResponse>> getTopCustomers() {
        return ResponseEntity.ok(reportService.getTopCustomers());
    }
}
