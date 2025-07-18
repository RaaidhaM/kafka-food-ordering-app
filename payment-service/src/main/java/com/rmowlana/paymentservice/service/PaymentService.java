package com.rmowlana.paymentservice.service;

import com.rmowlana.avroschemaandregistry.dto.InventoryStatus;
import com.rmowlana.avroschemaandregistry.dto.Payment;
import com.rmowlana.avroschemaandregistry.dto.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.rmowlana.avroschemaandregistry.dto.Inventory;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private KafkaTemplate<String, Payment> template;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

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
}
