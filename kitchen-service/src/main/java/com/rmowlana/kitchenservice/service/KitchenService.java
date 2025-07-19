package com.rmowlana.kitchenservice.service;

import com.rmowlana.avroschemaandregistry.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KitchenService {

    @Autowired
    private KafkaTemplate<String, Kitchen> template;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    // This enables the retry mechanism for the Kafka listener - 3 attempts with a backoff strategy
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 15000),
            exclude = {NullPointerException.class}
    )
    @KafkaListener(topics = "${inventory.topic.name}", groupId = "kitchen-group")
    private void verifyInventoryStatus (Inventory inventory ) {
        log.info("Kitchen Service Received The Order");
        if (inventory.getStatus() == InventoryStatus.IN_STOCK) {
            // Step 1: Publish PREPARING status
            publishKitchenStatus(inventory, KitchenStatus.PREPARING);

            // Step 2: Simulate preparation time
            simulateCooking();

            // Step 2: Publish READY status
            publishKitchenStatus(inventory, KitchenStatus.READY);

        }else if (inventory.getStatus() == InventoryStatus.OUT_OF_STOCK) {
            log.warn("Order " + inventory.getOrderId() + " is out of stock. Kitchen will not proceed.");
        }
    }

    private void publishKitchenStatus(Inventory inventoryStatus, KitchenStatus status) {
        Kitchen event = Kitchen.newBuilder()
                .setOrderId(inventoryStatus.getOrderId())
                .setStatus(status)
                .build();

        template.send(topicName, event.getOrderId().toString(), event);
        log.info("Kitchen Status " + " for Order " + event.getOrderId() + " Updated: " + status );
    }

    private void simulateCooking() {
        try {
            Thread.sleep(3000); // Simulate 3 seconds cooking time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
