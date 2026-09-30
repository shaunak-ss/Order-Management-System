package com.example.ordermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A sellable product with a monetary price (BigDecimal - never double/float)
 * and an available stock quantity.
 *
 * <p>Stock consistency under concurrent order placement is guarded by pessimistic
 * row locking (see OrderService / ProductRepository#findAllByIdInForUpdate), not by
 * an in-memory check, since two threads must never both observe stale stock and both
 * succeed.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Deducts the given quantity from stock. Callers are responsible for locking the
     * product row (see ProductRepository#findAllByIdInForUpdate) before calling this,
     * and for verifying sufficient stock first via {@link #hasSufficientStock(int)}.
     */
    public void deductStock(int quantity) {
        this.stock -= quantity;
    }

    public boolean hasSufficientStock(int quantity) {
        return this.stock >= quantity;
    }
}
