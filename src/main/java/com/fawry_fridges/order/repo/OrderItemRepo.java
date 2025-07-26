package com.fawry_fridges.order.repo;

import com.fawry_fridges.order.enities.OrderEntity;
import com.fawry_fridges.order.enities.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepo  extends JpaRepository<OrderItemEntity, String> {
}
