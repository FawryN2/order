package com.fawry_fridges.order.repo;

import com.fawry_fridges.order.enities.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public  interface OrderRepo extends JpaRepository<OrderEntity, String> {
    @Query("SELECT COALESCE(MAX(o.orderNumber), 0) FROM OrderEntity o")
    Long findMaxOrderNumber();
    Page<OrderEntity> findByUserIdAndCreatedAtBetween(String userId,
                                                      LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      Pageable pageable);
}
