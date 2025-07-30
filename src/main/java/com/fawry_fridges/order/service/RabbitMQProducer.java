package com.fawry_fridges.order.service;
import com.fawry_fridges.order.configrations.RabbitMq;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
@Service

public class RabbitMQProducer {
    private final AmqpTemplate amqpTemplate;

    public RabbitMQProducer(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void send(String message) {
        amqpTemplate.convertAndSend(RabbitMq.EXCHANGE_NAME, RabbitMq.ROUTING_KEY, message);
        System.out.println("Sent message: " + message);
    }
}
