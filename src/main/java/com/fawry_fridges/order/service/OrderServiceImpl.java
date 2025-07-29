package com.fawry_fridges.order.service;

import com.fawry_fridges.order.clients.ProductClient;
import com.fawry_fridges.order.dto.CouponDto;
import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.dto.OrderItemDto;
import com.fawry_fridges.order.enities.OrderEntity;
import com.fawry_fridges.order.enities.OrderItemEntity;

import com.fawry_fridges.order.enities.OrderStatus;

import com.fawry_fridges.order.error.OrderApiException;
import com.fawry_fridges.order.mapper.OrderMapper;
import com.fawry_fridges.order.repo.OrderItemRepo;
import com.fawry_fridges.order.repo.OrderRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements com.fawry_fridges.order.service.impl.OrderService {

    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepository;
    private final ProductClient productClient;
    private final RestTemplate restTemplate;

    @Value("${productsService.url}")
    private String productsUrl;

    @Value("${notificationService.url}")
    private String notificationsUrl;

    @Value("${couponService.url}")
    private String couponUrl;

    @Value("${stockService.url}")
    private String stockUrl;

    @Value("${bankService.url}")
    private String bankService;

    // ------------------ Public API ----------------------

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto dto) {
        validateOrder(dto);

        // Reserve stock
        reserveStock(dto.getItems());

        // Withdraw from customer
        String withdrawalTxnId = withdrawFromCustomer(dto.getUserId(), dto.getTotalPrice(), dto);

        // Deposit to merchant
        depositToMerchant(dto.getMerchantId(), dto.getTotalPrice(), dto);

        // Save order
        OrderEntity order = OrderMapper.INSTANCE.toEntity(dto);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setWithdrawalTxnId(withdrawalTxnId);
        order = orderRepo.save(order);

        for (OrderItemEntity item : order.getItems()) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }

        // Send notification
        sendNotification(dto.getUserId(), dto.getMerchantId());

        return OrderMapper.INSTANCE.toDto(order);
    }

    @Override
    public String checkout(OrderDto order, String userId, String shippingAddressId, String paymentMethod, Long couponId) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new OrderApiException("Cart is empty");
        }

        // Calculate total price
        double couponPercent = 0;
        if (order.getCouponId() != null && !order.getCouponId().trim().isEmpty()) {
            couponPercent = getCouponPercent(order);
        }

        orderTotalPrice(order);
        double orderPriceAfterCoupon = order.getTotalPrice() - (couponPercent * order.getTotalPrice());
        order.setTotalPrice(orderPriceAfterCoupon);

        // Optionally call createOrder() to finalize
        OrderDto createdOrder = createOrder(order);
        return createdOrder.getId();
    }

    @Override
    public Page<OrderDto> searchOrders(String customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<OrderEntity> page = orderRepo.findByUserIdAndCreatedAtBetween(customerId, startDate, endDate, pageable);
        return page.map(OrderMapper.INSTANCE::toDto);
    }

    @Override
    public OrderDto getOrderById(String orderId) {
        try {
            OrderEntity order = orderRepo.getReferenceById(orderId);
            return OrderMapper.INSTANCE.toDto(order);
        } catch (Exception e) {
            throw new OrderApiException("No order with this id");
        }
    }

    // ------------------ Private Helpers ----------------------

    private void validateOrder(OrderDto orderDto) {
        if (orderDto == null || orderDto.getItems().isEmpty()) {
            throw new OrderApiException("Order must contain at least one item");
        }
    }

    private void orderTotalPrice(OrderDto orderDto) {
        double itemsTotal = orderDto.getItems().stream()
                .mapToDouble(OrderItemDto::getFinalPrice)
                .sum();

        orderDto.setTotalPrice(itemsTotal + orderDto.getShippingCost());

        double discountTotal = orderDto.getItems().stream()
                .mapToDouble(item -> (item.getUnitPrice() * item.getQuantity()) - item.getFinalPrice())
                .sum();

        orderDto.setDiscountTotal(discountTotal);
    }

    private double getCouponPercent(OrderDto dto) {
        if (dto.getCouponId() != null && !dto.getCouponId().trim().isEmpty()) {
            try {
                CouponDto couponDto = restTemplate.getForObject(couponUrl + dto.getCouponId(), CouponDto.class);
                return couponDto != null ? couponDto.getPercent() : 0;
            } catch (RestClientException e) {
                throw new OrderApiException("Failed to process coupon: " + e.getMessage());
            }
        }
        return 0;
    }

    private void reserveStock(List<OrderItemDto> items) {
        try {
            restTemplate.postForObject(stockUrl + "/reserve", items, Void.class);
        } catch (RestClientException e) {
            throw new OrderApiException("Stock reservation failed: " + e.getMessage());
        }
    }

    private void releaseStock(List<OrderItemDto> items) {
        try {
            restTemplate.postForObject(stockUrl + "/release", items, Void.class);
        } catch (RestClientException e) {
            log.error("Failed to release stock: {}", e.getMessage());
        }
    }

    private String withdrawFromCustomer(String userId, double amount, OrderDto dto) {
        try {
            return restTemplate.postForObject(
                    bankService + "/withdraw?userId=" + userId + "&amount=" + amount,
                    null,
                    String.class
            );
        } catch (RestClientException e) {
            releaseStock(dto.getItems());
            throw new OrderApiException("Payment withdrawal failed: " + e.getMessage());
        }
    }

    private void refundCustomer(String userId, double amount) {
        try {
            restTemplate.postForObject(
                    bankService + "/refund?userId=" + userId + "&amount=" + amount,
                    null,
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to refund customer: {}", e.getMessage());
        }
    }

    private void depositToMerchant(String merchantId, double amount, OrderDto dto) {
        try {
            restTemplate.postForObject(
                    bankService + "/deposit?merchantId=" + merchantId + "&amount=" + amount,
                    null,
                    Void.class
            );
        } catch (RestClientException e) {
            refundCustomer(dto.getUserId(), dto.getTotalPrice());
            releaseStock(dto.getItems());
            throw new OrderApiException("Merchant deposit failed: " + e.getMessage());
        }
    }

    private void sendNotification(String userId, String merchantId) {
        try {
            restTemplate.postForObject(
                    notificationsUrl + "/send?userId=" + userId + "&merchantId=" + merchantId,
                    null,
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }
}
