package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.exception.DuplicateResourceException;
import com.example.ordermanagement.exception.ResourceNotFoundException;
import com.example.ordermanagement.mapper.CustomerMapper;
import com.example.ordermanagement.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        assertEmailAvailable(request.getEmail(), null);
        assertPhoneAvailable(request.getPhone(), null);

        Customer customer = Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={}", saved.getId());
        return CustomerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = getCustomerOrThrow(id);
        assertEmailAvailable(request.getEmail(), id);
        assertPhoneAvailable(request.getPhone(), id);

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        Customer saved = customerRepository.save(customer);
        log.info("Updated customer id={}", saved.getId());
        return CustomerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return CustomerMapper.toResponse(getCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(CustomerMapper::toResponse).toList();
    }

    private Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Customer", id));
    }

    private void assertEmailAvailable(String email, Long excludingCustomerId) {
        boolean exists = excludingCustomerId == null
                ? customerRepository.existsByEmail(email)
                : customerRepository.existsByEmailAndIdNot(email, excludingCustomerId);
        if (exists) {
            throw new DuplicateResourceException("A customer with email '" + email + "' already exists");
        }
    }

    private void assertPhoneAvailable(String phone, Long excludingCustomerId) {
        boolean exists = excludingCustomerId == null
                ? customerRepository.existsByPhone(phone)
                : customerRepository.existsByPhoneAndIdNot(phone, excludingCustomerId);
        if (exists) {
            throw new DuplicateResourceException("A customer with phone '" + phone + "' already exists");
        }
    }
}
