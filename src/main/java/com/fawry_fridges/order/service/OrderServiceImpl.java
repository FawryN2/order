package com.fawry_fridges.order.service;

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
import com.fawry_fridges.order.service.impl.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepository;
    private final RestTemplate restTemplate;
    private final OrderMessageSender orderMessageSender;

    @Value("${notificationService.url}")
    private String notificationsUrl;

    @Value("${couponService.url}")
    private String couponUrl;

    @Value("${stockService.url}")
    private String stockUrl;

    @Value("${bankService.url}")
    private String bankService;
    @Autowired
    private OrderMapper orderMapper;

    // ------------------ Public API ----------------------

    @Override
    @Transactional
    public OrderDto createOrder(OrderDto dto) {
        Long maxOrderNumber = orderRepo.findMaxOrderNumber();
dto.setOrderNumber(maxOrderNumber+1);
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


        // Step 4: Save order
        OrderEntity order = orderMapper.toEntity(dto);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepo.save(order);

        for (OrderItemEntity item : order.getItems()) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }


        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public String checkout(String orderId, String userId, String shippingAddressId, String paymentMethod, Long couponId) {
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderApiException("Order not found with id: " + orderId));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new OrderApiException("Cart is empty");
        }

        // Apply coupon
        double couponPercent = 0;
        if (order.getCouponName() != null && !order.getCouponName().trim().isEmpty()) {
            couponPercent = getCouponPercent(orderMapper.toDto(order));
        }

        OrderDto orderDto = orderMapper.toDto(order);
        double totalPrice = orderTotalPrice(orderDto);
        double discountedPrice = totalPrice - (couponPercent / 100 * totalPrice);
        order.setTotalPrice(discountedPrice);

        // Withdraw from user
        String withdrawalTxnId = withdrawFromCustomer(userId, discountedPrice, orderDto);
        order.setWithdrawalTxnId(withdrawalTxnId);

        // Deposit to merchant
        depositToMerchant(order.getMerchantId(), discountedPrice, orderDto);

        // Send notification
        sendNotification(userId, order.getMerchantId());

        // Send to RabbitMQ
        orderMessageSender.sendOrderPlacedEvent(order.getId());

        // Confirm order
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);

        return order.getId();
    }



    @Override
    public Page<OrderDto> searchOrders(String customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<OrderEntity> page = orderRepo.findByUserIdAndCreatedAtBetween(customerId, startDate, endDate, pageable);
        return page.map(orderMapper::toDto);
    }

    @Override
    public OrderDto getOrderById(String orderId) {
        log.info("Trying to find order with ID: {}", orderId);
        Optional<OrderEntity> optionalOrder = orderRepo.findById(orderId);

        if (optionalOrder.isEmpty()) {
            log.error("Order with ID {} not found in DB", orderId);
            throw new OrderApiException("No order with this id");
        }

        log.info("Order found: {}", optionalOrder.get());
        return orderMapper.toDto(optionalOrder.get());
    }



    // ------------------ Private Helpers ----------------------

    private void validateOrder(OrderDto orderDto) {
        if (orderDto == null || orderDto.getItems().isEmpty()) {
            throw new OrderApiException("Order must contain at least one item");
        }
    }

    private double orderTotalPrice(OrderDto orderDto) {
        double itemsTotal = orderDto.getItems().stream()
                .mapToDouble(OrderItemDto::getFinalPrice)
                .sum();

        orderDto.setTotalPrice(itemsTotal + orderDto.getShippingCost());

        double discountTotal = orderDto.getItems().stream()
                .mapToDouble(item -> (item.getUnitPrice() * item.getQuantity()) - item.getFinalPrice())
                .sum();

        orderDto.setDiscountTotal(discountTotal);

        return orderDto.getTotalPrice(); // ✅ now it returns the calculated total
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
                String url = UriComponentsBuilder.fromHttpUrl(couponUrl + "/consume")
                        .queryParam("code", dto.getCouponId())
                        .queryParam("orderId", dto.getId()) // assuming dto.getId() returns order UUID
                        .queryParam("orderTotal", dto.getTotalPrice())
                        .toUriString();

                ResponseEntity<CouponDto> response = restTemplate.postForEntity(url, null, CouponDto.class);

                CouponDto couponDto = response.getBody();
                return couponDto != null ? couponDto.getDiscount() : 0;
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