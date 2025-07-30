package com.fawry_fridges.order.controller;

import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.dto.requests.CheckoutRequest;
import com.fawry_fridges.order.dto.response.CheckoutResponse;
import com.fawry_fridges.order.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto orderDto) {
        log.info("Creating new order for user: {}", orderDto.getUserId());

        try {
            OrderDto createdOrder = orderService.createOrder(orderDto);
            log.info("Order created successfully with ID: {}", createdOrder.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Checkout and create order
     * POST /api/orders/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {

        log.info("Processing checkout for user: {}", request.getUserId());

        try {
            String orderId = orderService.checkout(
                    request.getOrder(),
                    request.getUserId(),
                    request.getShippingAddressId(),
                    request.getPaymentMethod(),
                    request.getCouponId()
            );

            CheckoutResponse response = new CheckoutResponse();
            response.setOrderId(orderId);
            response.setMessage("Order placed successfully");
            response.setStatus("SUCCESS");

            log.info("Checkout completed successfully. Order ID: {}", orderId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Checkout failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable String orderId) {
        log.info("Fetching order with ID: {}", orderId);

        try {
            OrderDto order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage());
            throw e;
        }
    }

    /**
     * Search orders with filters and pagination
     * GET /api/orders/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<OrderDto>> searchOrders(
            @RequestParam String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Searching orders for customer: {}, page: {}, size: {}", customerId, page, size);

        try {
            if (startDate == null) startDate = LocalDate.now().minusMonths(6);
            if (endDate == null) endDate = LocalDate.now();

            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.atTime(LocalTime.MAX);
            log.info("Searching from {} to {} for customer {}", from, to, customerId);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<OrderDto> orders = orderService.searchOrders(customerId, from, to, pageable);  // customerId will be used as userId

//            Page<OrderDto> orders = orderService.searchOrders(customerId, from, to, pageable);

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Failed to search orders: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get orders for a specific customer (simplified version)
     * GET /api/orders/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<OrderDto>> getCustomerOrders(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching orders for customer: {}", customerId);

        try {
            LocalDateTime startDate = LocalDateTime.now().minusYears(1); // Last year
            LocalDateTime endDate = LocalDateTime.now();

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<OrderDto> orders = orderService.searchOrders(customerId, startDate, endDate, pageable);

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Failed to fetch customer orders: {}", e.getMessage());
            throw e;
        }
    }



}