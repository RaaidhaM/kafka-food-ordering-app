package com.rmowlana.orderservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopic {
    @Value("${spring.kafka.topic.name}")
    private String topicName;

    @Bean
    public NewTopic createTopic() {
        // creating Kafka topic with 3 partitions and replication factor of 3
        return new NewTopic(topicName, 3, (short) 3);
    }
}
