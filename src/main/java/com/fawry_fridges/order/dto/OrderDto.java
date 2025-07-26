package com.fawry_fridges.order.dto;


import com.fawry_fridges.order.enities.OrderItemEntity;
import com.fawry_fridges.order.enities.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDto {
    private String id;
    private Long orderNumber;
    private String userId;
    private String shippingAddressId;
    private String paymentMethod;
    private OrderStatus status;
    private double totalPrice;
    private double discountTotal;
    private double shippingCost;
    private Long couponId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> items;
}
