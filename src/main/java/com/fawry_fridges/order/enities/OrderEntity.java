package com.fawry_fridges.order.enities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true)
    private Long orderNumber;

    private String userId;

    private String shippingAddressId;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private double totalPrice;

    private double discountTotal;

    private double shippingCost;

    private String couponName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String merchantId;
    private String withdrawalTxnId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items;
}
