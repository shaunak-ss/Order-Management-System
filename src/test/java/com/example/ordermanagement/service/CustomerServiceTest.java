package com.example.ordermanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.exception.DuplicateResourceException;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.repository.CustomerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createCustomer_success_returnsMappedResponse() {
        CustomerRequest request = new CustomerRequest("John Doe", "john@example.com", "1111111111");
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("1111111111")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void createCustomer_duplicateEmail_throwsDuplicateResourceException() {
        CustomerRequest request = new CustomerRequest("John Doe", "dup@example.com", "1111111111");
        when(customerRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
    }

    @Test
    void createCustomer_duplicatePhone_throwsDuplicateResourceException() {
        CustomerRequest request = new CustomerRequest("John Doe", "john@example.com", "2222222222");
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("2222222222")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
    }

    @Test
    void updateCustomer_success_updatesFields() {
        Customer existing = Customer.builder().id(1L).name("Old").email("old@example.com").phone("3333333333").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(customerRepository.existsByPhoneAndIdNot("4444444444", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerRequest request = new CustomerRequest("New Name", "new@example.com", "4444444444");
        CustomerResponse response = customerService.updateCustomer(1L, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getPhone()).isEqualTo("4444444444");
    }

    @Test
    void updateCustomer_nonExistent_throwsResourceNotFoundException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());
        CustomerRequest request = new CustomerRequest("Name", "email@example.com", "5555555555");

        assertThrows(ResourceNotFoundException.class, () -> customerService.updateCustomer(999L, request));
    }

    @Test
    void getCustomer_nonExistent_throwsResourceNotFoundException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomer(999L));
    }
}
