package com.rmowlana.orderservice.service;

import com.rmowlana.avroschemaandregistry.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, Order> template;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    public void placeOrder (Order order) {
        // Sending the order event to Kafka topic
        CompletableFuture<SendResult<String, Order>> result = template.send(topicName, UUID.randomUUID().toString(), order);
        result.whenComplete((r, ex) -> {
            if (ex != null) {
                System.err.println("Error sending order event: " + ex.getMessage());
            } else {
                System.out.println("Order event sent successfully =[" + order + "] with offset =[" + r.getRecordMetadata().offset() + "]");
            }
        });
    }
}
