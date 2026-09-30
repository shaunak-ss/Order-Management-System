package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Order;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.customer c
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.product
            WHERE o.customer.id = :customerId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("""
            SELECT c.id AS customerId, c.name AS customerName, COUNT(o.id) AS orderCount
            FROM Customer c
            LEFT JOIN Order o ON o.customer = c
            GROUP BY c.id, c.name
            ORDER BY c.id ASC
            """)
    List<CustomerOrderCountProjection> countOrdersPerCustomer();

    /**
     * Customers ranked by number of orders placed, descending, with the result window
     * limited at the database level via {@code pageable} (callers pass a page of size 5).
     * Only customers with at least one order are considered. Ties are broken by customer
     * id for a deterministic, reproducible ordering.
     */
    @Query("""
            SELECT c.id AS customerId, c.name AS customerName, COUNT(o.id) AS orderCount
            FROM Customer c
            JOIN Order o ON o.customer = c
            GROUP BY c.id, c.name
            ORDER BY COUNT(o.id) DESC, c.id ASC
            """)
    List<CustomerOrderCountProjection> findTopCustomersByOrderCount(Pageable pageable);
}
