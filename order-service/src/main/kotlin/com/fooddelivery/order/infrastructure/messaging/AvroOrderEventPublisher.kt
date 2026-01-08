package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.shared.avro.*
import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AvroOrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {
    private val logger = LoggerFactory.getLogger(AvroOrderEventPublisher::class.java)
    
    fun sendPrepareOrder(orderId: OrderId, items: List<OrderItem>, priority: Int = 0) {
        val command = KitchenCommand.PrepareOrder(
            orderId = orderId,
            items = items,
            priority = priority,
            timestamp = Instant.now()
        )
        
        val avroRecord = KitchenAvroMapper.toAvro(command)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Sending Avro PrepareOrder command for order $orderId to kitchen")
        kafkaTemplate.send(KafkaTopics.KITCHEN_COMMANDS, orderId.toString(), bytes)
    }
    
    fun sendCancelTicket(orderId: OrderId, reason: String) {
        val command = KitchenCommand.CancelTicket(
            orderId = orderId,
            reason = reason,
            timestamp = Instant.now()
        )
        
        val avroRecord = KitchenAvroMapper.toAvro(command)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Sending Avro CancelTicket command for order $orderId to kitchen")
        kafkaTemplate.send(KafkaTopics.KITCHEN_COMMANDS, orderId.toString(), bytes)
    }
    
    fun sendScheduleDelivery(orderId: OrderId, address: Address, estimatedPickupTime: Instant) {
        val command = DeliveryCommand.ScheduleDelivery(
            orderId = orderId,
            deliveryAddress = address,
            estimatedPickupTime = estimatedPickupTime,
            timestamp = Instant.now()
        )
        
        val avroRecord = DeliveryAvroMapper.toAvro(command)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Sending Avro ScheduleDelivery command for order $orderId")
        kafkaTemplate.send(KafkaTopics.DELIVERY_COMMANDS, orderId.toString(), bytes)
    }
    
    fun sendCancelDelivery(orderId: OrderId, reason: String) {
        val command = DeliveryCommand.CancelDelivery(
            orderId = orderId,
            reason = reason,
            timestamp = Instant.now()
        )
        
        val avroRecord = DeliveryAvroMapper.toAvro(command)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Sending Avro CancelDelivery command for order $orderId")
        kafkaTemplate.send(KafkaTopics.DELIVERY_COMMANDS, orderId.toString(), bytes)
    }
    
    fun publishOrderCreated(orderId: OrderId, customerId: CustomerId, totalAmount: MonetaryAmount) {
        val event = OrderEvent.OrderCreated(
            orderId = orderId,
            customerId = customerId,
            totalAmount = totalAmount
        )
        
        val avroRecord = OrderAvroMapper.toAvro(event)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Publishing Avro OrderCreated event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), bytes)
    }
    
    fun publishOrderCompleted(orderId: OrderId) {
        val event = OrderEvent.OrderCompleted(orderId = orderId)
        
        val avroRecord = OrderAvroMapper.toAvro(event)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Publishing Avro OrderCompleted event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), bytes)
    }
    
    fun publishOrderCancelled(orderId: OrderId, reason: String) {
        val event = OrderEvent.OrderCancelled(orderId = orderId, reason = reason)
        
        val avroRecord = OrderAvroMapper.toAvro(event)
        val bytes = AvroSerializer.serialize(avroRecord)
        
        logger.info("Publishing Avro OrderCancelled event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), bytes)
    }
}
