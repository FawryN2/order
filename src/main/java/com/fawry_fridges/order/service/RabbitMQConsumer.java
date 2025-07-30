package com.fawry_fridges.order.service;
import com.fawry_fridges.order.configrations.RabbitMq;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {

    @RabbitListener(queues = RabbitMq.QUEUE_NAME)
    public void receive(String message) {
        System.out.println("Received message: " + message);
    }
}
