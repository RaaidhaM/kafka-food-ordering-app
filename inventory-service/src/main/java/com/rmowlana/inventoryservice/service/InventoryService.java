package com.rmowlana.inventoryservice.service;

import com.rmowlana.avroschemaandregistry.dto.Inventory;
import com.rmowlana.avroschemaandregistry.dto.InventoryStatus;
import org.springframework.messaging.handler.annotation.Header;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import com.rmowlana.avroschemaandregistry.dto.Order;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    public KafkaTemplate<String, Inventory> template;

    // Simple in-memory stock simulation
    private final Map<String, Integer> stockDB = new HashMap<>() {{
        put("burger", 10);
        put("pizza", 5);
        put("buns", 20);
        put("fries", 15);
        put("cake", 30);
        put("drinks", 0);
    }};

    @Value("${spring.kafka.topic.name}")
    private String notificationTopicName;

    public void  publishInventoryStatus (Inventory inventory) {
        // Sending inventory status to the Inventory topic
        template.send(notificationTopicName, inventory.getOrderId().toString(), inventory);
        log.info("Inventory Status Updated: " + inventory.getStatus() + " for Order ID: " + inventory.getOrderId());
    }

    // This enables the retry mechanism for the Kafka listener - 3 attempts with a backoff strategy
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 15000),
            exclude = {NullPointerException.class}
    )
    @KafkaListener(topics = "${order.topic.name}", groupId = "inventory-group")
    public void handleOrder (Order order) {
        log.info("Inventory Service Received The Order");
        String productId = order.getProductId().toString();
        int quantity = order.getQuantity();

        Inventory inventoryStatus;

        if (stockDB.getOrDefault(productId, 0) >= quantity) {
            // Reduce stock
            stockDB.put(productId, stockDB.get(productId) - quantity);
            inventoryStatus = Inventory.newBuilder()
                    .setOrderId(order.getOrderId())
                    .setProductId(productId)
                    .setStatus(InventoryStatus.IN_STOCK)
                    .build();

        }else {
            inventoryStatus = Inventory.newBuilder()
                    .setOrderId(order.getOrderId())
                    .setProductId(productId)
                    .setStatus(InventoryStatus.OUT_OF_STOCK)
                    .build();
        }
        // Publish inventory status to topic
        publishInventoryStatus(inventoryStatus);
    }


    // This method is triggered when a message fails all retry attempts.
    // The failed messages are moved to a dead letter topic (DLT) for further investigation.
    @DltHandler //this enables dlt logic for our kafka topic
    public void listenDLT(Order order,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Inventory Service DLT: Failed to process Order ID {}, from topic: {}, offset: {}",
                order.getOrderId(), topic, offset);
        // Additional recovery logic could be implemented here:
        // - Send notifications to support team
        // - Store failed orders in a database for manual review
        // - Attempt alternative processing strategies
    }


}
