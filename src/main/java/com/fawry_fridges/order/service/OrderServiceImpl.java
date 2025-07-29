package com.fawry_fridges.order.service;

import com.fawry_fridges.order.clients.ProductClient;
import com.fawry_fridges.order.dto.CouponDto;
import com.fawry_fridges.order.dto.NotificationDto;
import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.dto.OrderItemDto;
import com.fawry_fridges.order.dto.requests.ConsumeStockRequest;
import com.fawry_fridges.order.dto.requests.CouponConsumptionRequest;
import com.fawry_fridges.order.dto.requests.DepositRequest;
import com.fawry_fridges.order.dto.requests.WithdrawRequest;
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

        // Step 1: Check availability of each item
        checkProductAvailability(dto.getItems());

        // Step 2: Consume stock for each item
        consumeStock(dto);

        // Step 3: Apply coupon (consume it if valid)
        String couponId = dto.getCouponId();
        if (couponId != null && !couponId.trim().isEmpty()) {
            consumeCoupon(couponId, dto.getUserId(), dto.getId());
        }

        // Step 4: Withdraw from customer
        String withdrawalTxnId = withdrawFromCustomer(dto.getUserId(), dto.getTotalPrice(), dto);

        // Step 5: Deposit to merchant
        depositToMerchant(dto.getMerchantId(), dto.getTotalPrice(), dto);

        // Step 6: Save order
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

        // Step 7: Send notification
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


    private void checkProductAvailability(List<OrderItemDto> items) {
        for (OrderItemDto item : items) {
            try {
                // Call GET /stocks/products/{productId}/availability
                String availabilityUrl = stockUrl + "/products/" + item.getProductId() + "/availability";
                Boolean isAvailable = restTemplate.getForObject(availabilityUrl, Boolean.class);

                if (isAvailable == null || !isAvailable) {
                    throw new OrderApiException("Product " + item.getProductId() + " is not available");
                }
            } catch (RestClientException e) {
                throw new OrderApiException("Failed to check availability for product " + item.getProductId() + ": " + e.getMessage());
            }
        }
    }

    private double getCouponPercent(OrderDto dto) {
        if (dto.getCouponId() != null && !dto.getCouponId().trim().isEmpty()) {
            try {
                CouponDto couponDto = restTemplate.getForObject(couponUrl + "/consume" + dto.getCouponId(), CouponDto.class);
                return couponDto != null ? couponDto.getPercent() : 0;
            } catch (RestClientException e) {
                throw new OrderApiException("Failed to process coupon: " + e.getMessage());
            }
        }
        return 0;
    }


    private void consumeCoupon(String couponId, String userId, String orderId) {
        try {
            // Create request body for coupon consumption
            CouponConsumptionRequest request = new CouponConsumptionRequest();
            request.setCustomerEmail(userId); // Assuming userId is email
            request.setOrderId(orderId);

            // Call POST /api/coupons/history to consume the coupon
            restTemplate.postForObject(couponUrl + "/history", request, Void.class);
        } catch (RestClientException e) {
            throw new OrderApiException("Failed to consume coupon: " + e.getMessage());
        }
    }


    private void consumeStock(OrderDto dto) {
        try {
            // For each item, call the consume stock API
            for (OrderItemDto item : dto.getItems()) {
                ConsumeStockRequest request = new ConsumeStockRequest();
                request.setQuantity(item.getQuantity());

                String consumeUrl = stockUrl + "/consume?storeId=" + dto.getMerchantId() + "&sku=" + item.getProductId();
                restTemplate.postForObject(consumeUrl, request, Void.class);
            }
        } catch (RestClientException e) {
            throw new OrderApiException("Stock consumption failed: " + e.getMessage());
        }
    }

    private String withdrawFromCustomer(String userId, double amount, OrderDto dto) {
        try {
            WithdrawRequest request = new WithdrawRequest();
            request.setCardNumber("4117394963435739"); // This should come from the order or user profile
            request.setAmount(amount);
            request.setMerchant("fawry"); // This should come from merchant info

            return restTemplate.postForObject(
                    bankService + "/transactions/withdraw",
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new OrderApiException("Payment withdrawal failed: " + e.getMessage());
        }
    }



    private void depositToMerchant(String merchantId, double amount, OrderDto dto) {
        try {
            DepositRequest request = new DepositRequest();
            request.setCardNumber("4117394963435739"); // This should come from merchant info
            request.setAmount(amount);

            restTemplate.postForObject(
                    bankService + "/transactions/deposit",
                    request,
                    Void.class
            );
        } catch (RestClientException e) {
            throw new OrderApiException("Merchant deposit failed: " + e.getMessage());
        }
    }

    private void sendNotification(String userId, String merchantId) {
        try {
            NotificationDto request = new NotificationDto();
            request.setTo("alhasan.ebrahim@outlook.com"); // This should come from user profile
            request.setSubject("shopping");
            request.setProduct("laptop"); // This should be dynamic based on order items
            request.setPrice(1000); // This should be the actual order total

            restTemplate.postForObject(
                    notificationsUrl ,
                    request,
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }








}