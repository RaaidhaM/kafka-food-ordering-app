package com.rmowlana.notificationservice.service;

import com.rmowlana.avroschemaandregistry.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "${inventory.topic.name}", groupId = "notification-group")
    public void handleInventoryStatus(Inventory inventoryStatus) {
        if (inventoryStatus.getStatus() == InventoryStatus.OUT_OF_STOCK) {
            sendNotification(inventoryStatus.getOrderId().toString(),
                    InventoryStatus.OUT_OF_STOCK.toString());
        } else {
            sendNotification(inventoryStatus.getOrderId().toString(),
                    InventoryStatus.IN_STOCK.toString());
        }
    }

    @KafkaListener(topics = "${payment.topic.name}", groupId = "notification-group")
    public void handlePaymentStatus(Payment paymentStatus) {
        if (paymentStatus.getStatus() == PaymentStatus.PAYMENT_SUCCESS) {
            sendNotification(paymentStatus.getOrderId().toString(),
                    PaymentStatus.PAYMENT_SUCCESS.toString());
        } else if (paymentStatus.getStatus() == PaymentStatus.PAYMENT_FAILED) {
            sendNotification(paymentStatus.getOrderId().toString(),
                    PaymentStatus.PAYMENT_FAILED.toString());
        } else {
            sendNotification(paymentStatus.getOrderId().toString(),
                        "Payment skipped as item OUT_OF_STOCK");
        }
    }

    @KafkaListener(topics = "${kitchen.topic.name}", groupId = "notification-group")
    public void handleKitchenStatus(Kitchen kitchenStatus) {
        if (kitchenStatus.getStatus() == KitchenStatus.PREPARING) {
            sendNotification(kitchenStatus.getOrderId().toString(),
                    KitchenStatus.PREPARING.toString());
        } else if (kitchenStatus.getStatus() == KitchenStatus.READY) {
            sendNotification(kitchenStatus.getOrderId().toString(),
                    KitchenStatus.READY.toString());
        }
    }

    private void sendNotification(String orderId, String message) {
        log.info("Order " + orderId + " Update: " + message);
    }

}
