package com.example.ordermanagement.repository;

public interface CustomerOrderCountProjection {

    Long getCustomerId();

    String getCustomerName();

    Long getOrderCount();
}
