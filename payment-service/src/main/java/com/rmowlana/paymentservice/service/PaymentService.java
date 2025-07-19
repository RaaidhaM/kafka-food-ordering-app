package com.rmowlana.paymentservice.service;

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
public class PaymentService {

    @Autowired
    private KafkaTemplate<String, Payment> template;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    // This enables the retry mechanism for the Kafka listener - 3 attempts with a backoff strategy
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 15000),
            exclude = {NullPointerException.class}
    )
    @KafkaListener(topics = "${inventory.topic.name}", groupId = "payment-group")
    private void verifyInventoryStatus (Inventory inventory ) {
        log.info("Payment Service Received The Order");
        if (inventory.getStatus() == InventoryStatus.IN_STOCK) {
            // Simulate payment processing
            boolean paymentProcessed = processPayment(inventory.getOrderId().toString());

            if (paymentProcessed) {
                publishPaymentStatus(inventory, PaymentStatus.PAYMENT_SUCCESS);
            } else {
                publishPaymentStatus(inventory, PaymentStatus.PAYMENT_FAILED);
            }

        }else if (inventory.getStatus() == InventoryStatus.OUT_OF_STOCK) {
            log.warn("Order " + inventory.getOrderId() + " is out of stock. Payment will not be processed.");
        }
    }

    private boolean processPayment(String orderId) {
        log.info("Processing payment for Order: " + orderId);
        try {
            Thread.sleep(2000); // Simulate payment delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    private void publishPaymentStatus(Inventory inventoryStatus, PaymentStatus status) {
        Payment event = Payment.newBuilder()
                .setOrderId(inventoryStatus.getOrderId())
                .setStatus(status)
                .build();

        template.send(topicName, event.getOrderId().toString(), event);
        log.info("Payment Status " + " for Order " + event.getOrderId() + " Updated: " + status );
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
