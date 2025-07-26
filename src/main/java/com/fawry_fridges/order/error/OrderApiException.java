package com.fawry_fridges.order.error;

public class OrderApiException extends RuntimeException {
    public OrderApiException(String message) {
        super(message);
    }


  public OrderApiException() {
    super();
  }
}
