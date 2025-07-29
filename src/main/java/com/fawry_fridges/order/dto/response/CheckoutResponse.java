package com.fawry_fridges.order.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckoutResponse {
    private String orderId;
    private String message;
    private String status;
    private LocalDateTime timestamp;

    public CheckoutResponse() {
        this.timestamp = LocalDateTime.now();
    }
}
