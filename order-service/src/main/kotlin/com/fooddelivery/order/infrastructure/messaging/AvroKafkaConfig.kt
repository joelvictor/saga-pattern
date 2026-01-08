package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.shared.avro.AvroSchemas
import com.fooddelivery.shared.avro.AvroSerializer
import com.fooddelivery.shared.events.KafkaTopics
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.*

@Configuration
class AvroKafkaConfig {
    
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String
    
    @Bean
    fun kitchenCommandsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.KITCHEN_COMMANDS)
            .partitions(3)
            .replicas(1)
            .build()
    }
    
    @Bean
    fun deliveryCommandsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.DELIVERY_COMMANDS)
            .partitions(3)
            .replicas(1)
            .build()
    }
    
    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS)
            .partitions(3)
            .replicas(1)
            .build()
    }
    
    @Bean
    @Primary
    fun avroProducerFactory(): ProducerFactory<String, ByteArray> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3
        )
        return DefaultKafkaProducerFactory(config)
    }
    
    @Bean
    @Primary
    fun avroKafkaTemplate(producerFactory: ProducerFactory<String, ByteArray>): KafkaTemplate<String, ByteArray> {
        return KafkaTemplate(producerFactory)
    }
    
    @Bean
    fun avroConsumerFactory(): ConsumerFactory<String, ByteArray> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false
        )
        return DefaultKafkaConsumerFactory(config)
    }
    
    @Bean
    fun avroKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, ByteArray>
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(3)
        return factory
    }
}
