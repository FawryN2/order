package com.fawry_fridges.order.service;

import com.fawry_fridges.order.configrations.RabbitMq;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderMessageSender {

    private final AmqpTemplate amqpTemplate;

    public void sendOrderPlacedEvent(String orderId) {
        amqpTemplate.convertAndSend(RabbitMq.EXCHANGE_NAME, RabbitMq.ROUTING_KEY, orderId);
    }
}