package com.example.ordermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanagement.dto.response.CustomerOrderCountResponse;
import com.example.ordermanagement.repository.CustomerOrderCountProjection;
import com.example.ordermanagement.repository.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getOrdersPerCustomer_mapsProjectionsToResponses() {
        CustomerOrderCountProjection projection = projection(1L, "Alice", 3L);
        when(orderRepository.countOrdersPerCustomer()).thenReturn(List.of(projection));

        List<CustomerOrderCountResponse> result = reportService.getOrdersPerCustomer();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(1L);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice");
        assertThat(result.get(0).getOrderCount()).isEqualTo(3L);
    }

    @Test
    void getTopCustomers_requestsTopFivePage() {
        when(orderRepository.findTopCustomersByOrderCount(PageRequest.of(0, 5)))
                .thenReturn(List.of(projection(1L, "Alice", 5L), projection(2L, "Bob", 3L)));

        List<CustomerOrderCountResponse> result = reportService.getTopCustomers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderCount()).isEqualTo(5L);
        assertThat(result.get(1).getOrderCount()).isEqualTo(3L);
    }

    private CustomerOrderCountProjection projection(Long id, String name, Long count) {
        return new CustomerOrderCountProjection() {
            @Override
            public Long getCustomerId() {
                return id;
            }

            @Override
            public String getCustomerName() {
                return name;
            }

            @Override
            public Long getOrderCount() {
                return count;
            }
        };
    }
}
