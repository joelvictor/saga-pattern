package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.shared.events.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/**
 * Kafka configuration for Order Service.
 */
@Configuration
class KafkaConfig {
    
    /**
     * Create kitchen commands topic.
     */
    @Bean
    fun kitchenCommandsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.KITCHEN_COMMANDS)
            .partitions(3)
            .replicas(1)
            .build()
    }
    
    /**
     * Create delivery commands topic.
     */
    @Bean
    fun deliveryCommandsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.DELIVERY_COMMANDS)
            .partitions(3)
            .replicas(1)
            .build()
    }
    
    /**
     * Create order events topic.
     */
    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS)
            .partitions(3)
            .replicas(1)
            .build()
    }
}
