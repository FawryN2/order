package com.fawry_fridges.order.dto.requests;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponConsumptionRequest {
    private String customerEmail;
    private String orderId;

}
