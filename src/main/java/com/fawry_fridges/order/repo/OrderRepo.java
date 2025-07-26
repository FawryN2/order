package com.fawry_fridges.order.repo;

import com.fawry_fridges.order.enities.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public  interface OrderRepo extends JpaRepository<OrderEntity, String> {

    Page<OrderEntity> findByUserIdAndCreatedAtBetween(String customerId,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      Pageable pageable);
}
