package com.rmowlana.kitchenservice.service;

import com.rmowlana.avroschemaandregistry.dto.InventoryStatus;
import com.rmowlana.avroschemaandregistry.dto.Kitchen;
import com.rmowlana.avroschemaandregistry.dto.KitchenStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.rmowlana.avroschemaandregistry.dto.Inventory;

@Service
@Slf4j
public class KitchenService {

    @Autowired
    private KafkaTemplate<String, Kitchen> template;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

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
}
