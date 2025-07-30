package com.fawry_fridges.order.dto.requests;

import com.fawry_fridges.order.dto.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    private String orderId;
    private String userId;
    private String shippingAddressId;
    private String paymentMethod;
    private Long couponId;

}
