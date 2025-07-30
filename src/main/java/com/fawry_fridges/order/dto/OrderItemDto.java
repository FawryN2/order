package com.fawry_fridges.order.dto;

import com.fawry_fridges.order.enities.OrderEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemDto {

    private String id;
//    private OrderEntity order;
    private String productId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double discountApplied;
    private double finalPrice;

}
