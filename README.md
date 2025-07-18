# Kafka Food Ordering System
This project demonstrates a Kafka-based event-driven architecture for a simple Online Food Ordering System.

It shows how multiple microservices communicate asynchronously using Kafka topics, Avro schemas, and Schema Registry.

## Modules
- **kafka-infra**: Contains the Docker Compose setup for Kafka infrastructure.
- **avro-schema-and-registry**: Defines Avro schemas for the messages exchanged between services and sets up the Schema Registry.
- **Order Service**: Handles order creation and management.
- **inventory-service**: Manages inventory and stock levels.
- **payment-service**: Processes payments for orders.
- **kitchen-service**: Manages the preparation of food items.
- **notification-service**: Sends notifications to users about order status.

## Service Flow
### Order Service
- Receives customer order
- Publishes it to order-topic

### Inventory Service
- Consumes order-topic
- Checks stock availability
- Publishes status (IN_STOCK / OUT_OF_STOCK) to inventory-topic

### Payment & Kitchen Service
- Both consume inventory-topic
- Proceed only if stock is available
- Publish updates to payment-topic & kitchen-topic

### Notification Service
- Listens to all three topics
- Sends real-time updates to the customer

## How to Run
1. Start the Kafka infrastructure:
   ```bash
   cd kafka-infra
   docker-compose up -d
   ```
2. Build Avro schemas:
   ```bash
    cd avro-schema-and-registry
    mvn clean install
    ```
3. Start each service:
    - Order Service:
      ```bash
      cd order-service
      mvn spring-boot:run
      ```
    - Inventory Service:
      ```bash
      cd inventory-service
      mvn spring-boot:run
      ```
    - Payment Service:
      ```bash
      cd payment-service
      mvn spring-boot:run
      ```
    - Kitchen Service:
      ```bash
      cd kitchen-service
      mvn spring-boot:run
      ```
    - Notification Service:
      ```bash
      cd notification-service
      mvn spring-boot:run
      ```
4. Place an order using the Order Service API:
    ```bash
   POST http://localhost:8081/order
   ```
5. Watch Kafka topics update via Control Center.