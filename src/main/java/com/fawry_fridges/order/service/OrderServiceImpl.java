package com.fawry_fridges.order.service;
import com.fawry_fridges.order.enities.OrderStatus;

import com.fawry_fridges.order.dto.OrderDto;
import com.fawry_fridges.order.dto.OrderItemDto;
import com.fawry_fridges.order.enities.OrderEntity;
import com.fawry_fridges.order.enities.OrderItemEntity;
import com.fawry_fridges.order.error.OrderApiException;
import com.fawry_fridges.order.mapper.OrderMapper;
import com.fawry_fridges.order.repo.OrderItemRepo;
import com.fawry_fridges.order.repo.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements com.fawry_fridges.order.service.impl.OrderService {

    @Autowired
    OrderRepo orderRepo;
    @Autowired
    private OrderItemRepo orderItemRepository;

    @Override
    public OrderDto createOrder(OrderDto dto) {
        // 1) Validate & consume coupon
        if (dto.getCouponId() != null) {
            // TODO: call coupon-api to validate and consume coupon
        }

        // 2) Reserve stock
        // TODO: call store-api to decrement inventory

        // 3) Withdraw from customer
        // TODO: call bank-api to withdraw amount from customer and set withdrawalTxnId

        // 4) Deposit to merchant
        // TODO: call bank-api to deposit amount to merchant

        OrderEntity order = OrderMapper.INSTANCE.toEntity(dto);
        order.setStatus( OrderStatus.CONFIRMED );
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepo.save(order);

        for (OrderItemEntity item : order.getItems()) {
            item.setOrder(order);
            orderItemRepository.save(item);
        }

        // 6) Send notification
        // TODO: call notification-api to notify customer / merchant

        return OrderMapper.toDto(order);
    }


    @Override
    public Page<OrderDto> searchOrders(String customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        Page<OrderEntity> page = orderRepo
                .findByUserIdAndCreatedAtBetween(customerId, startDate, endDate, pageable);
        return page.map(OrderMapper::toDto);

    }

    @Override
    public List<Object> displayProducts() {

//      call product portal to fetch products of order
        return List.of();
    }

    @Override
    public String checkout(List<OrderItemDto> items, String userId, String shippingAddressId, String paymentMethod, Long couponId) {


        return "";
    }

    @Override
    public OrderDto getOrderById(String orderId) {


        try {
            final OrderEntity order =   orderRepo.getReferenceById(orderId);

            final OrderDto orderDto =  OrderMapper.toDto(order);
            return orderDto;
        } catch (Exception e) {
            throw new OrderApiException("No order with this id");
        }
    }





    private void validateOrder(OrderDto orderDto){

        if(orderDto ==null || orderDto.getItems().isEmpty()){
            throw new OrderApiException("Order must contain at least one item");
        }


//        هنا بقا هنفليديت لسه علي المنتجات الي جايه من البرودكتس API ونبصيها لل اودرد ايتم
    }


    private double calculateItemّFinalPrice(OrderItemDto item, boolean hasCoupon) {
        double priceAfterItemDiscount = item.getUnitPrice() - item.getDiscountApplied();
        return priceAfterItemDiscount * item.getQuantity();
    }

    private void orderTotalPrice(OrderDto orderDto, boolean hasCoupon) {
        double itemsTotal = orderDto.getItems().stream().mapToDouble(
                OrderItemDto::getFinalPrice
        ).sum();

        orderDto.setTotalPrice(itemsTotal + orderDto.getShippingCost());
        orderDto.setDiscountTotal(orderDto.getItems().stream()
                .mapToDouble(item ->
                        (item.getUnitPrice() * item.getQuantity()) - item.getFinalPrice())
                .sum());
    }

}
