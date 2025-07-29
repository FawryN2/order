package com.fawry_fridges.order.service.impl;

import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.dto.OrderItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

   OrderDto getOrderById(String orderId);
   OrderDto createOrder(OrderDto orderDto);
   Page<OrderDto> searchOrders(String userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
   String checkout(OrderDto order, String userId, String shippingAddressId, String paymentMethod, Long couponId);
//   List<Object> displayProducts();

}
