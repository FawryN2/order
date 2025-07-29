package com.fawry_fridges.order.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class DepositRequest {
    private String cardNumber;
    private double amount;

    // Getters and setters
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}