package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.shared.events.*
import com.fooddelivery.shared.domain.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Kafka producer for sending commands to Kitchen and Delivery services.
 */
@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(OrderEventPublisher::class.java)
    
    /**
     * Send prepare order command to Kitchen Service.
     */
    fun sendPrepareOrder(orderId: OrderId, items: List<OrderItem>, priority: Int = 0) {
        val command = KitchenCommand.PrepareOrder(
            orderId = orderId,
            items = items,
            priority = priority,
            timestamp = Instant.now()
        )
        
        logger.info("Sending PrepareOrder command for order $orderId to kitchen")
        kafkaTemplate.send(KafkaTopics.KITCHEN_COMMANDS, orderId.toString(), command)
    }
    
    /**
     * Send cancel ticket command to Kitchen Service.
     */
    fun sendCancelTicket(orderId: OrderId, reason: String) {
        val command = KitchenCommand.CancelTicket(
            orderId = orderId,
            reason = reason,
            timestamp = Instant.now()
        )
        
        logger.info("Sending CancelTicket command for order $orderId to kitchen")
        kafkaTemplate.send(KafkaTopics.KITCHEN_COMMANDS, orderId.toString(), command)
    }
    
    /**
     * Send schedule delivery command to Delivery Service.
     */
    fun sendScheduleDelivery(orderId: OrderId, address: Address, estimatedPickupTime: Instant) {
        val command = DeliveryCommand.ScheduleDelivery(
            orderId = orderId,
            deliveryAddress = address,
            estimatedPickupTime = estimatedPickupTime,
            timestamp = Instant.now()
        )
        
        logger.info("Sending ScheduleDelivery command for order $orderId")
        kafkaTemplate.send(KafkaTopics.DELIVERY_COMMANDS, orderId.toString(), command)
    }
    
    /**
     * Send cancel delivery command to Delivery Service.
     */
    fun sendCancelDelivery(orderId: OrderId, reason: String) {
        val command = DeliveryCommand.CancelDelivery(
            orderId = orderId,
            reason = reason,
            timestamp = Instant.now()
        )
        
        logger.info("Sending CancelDelivery command for order $orderId")
        kafkaTemplate.send(KafkaTopics.DELIVERY_COMMANDS, orderId.toString(), command)
    }
    
    /**
     * Publish order created event.
     */
    fun publishOrderCreated(orderId: OrderId, customerId: CustomerId, totalAmount: MonetaryAmount) {
        val event = OrderEvent.OrderCreated(
            orderId = orderId,
            customerId = customerId,
            totalAmount = totalAmount
        )
        
        logger.info("Publishing OrderCreated event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), event)
    }
    
    /**
     * Publish order completed event.
     */
    fun publishOrderCompleted(orderId: OrderId) {
        val event = OrderEvent.OrderCompleted(orderId = orderId)
        
        logger.info("Publishing OrderCompleted event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), event)
    }
    
    /**
     * Publish order cancelled event.
     */
    fun publishOrderCancelled(orderId: OrderId, reason: String) {
        val event = OrderEvent.OrderCancelled(orderId = orderId, reason = reason)
        
        logger.info("Publishing OrderCancelled event for order $orderId")
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, orderId.toString(), event)
    }
}
