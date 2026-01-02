package com.fooddelivery.delivery.messaging

import com.fooddelivery.delivery.domain.Delivery
import com.fooddelivery.delivery.infrastructure.DeliveryRepository
import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class DeliveryCommandListener(
    private val deliveryRepository: DeliveryRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(DeliveryCommandListener::class.java)
    
    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_COMMANDS],
        groupId = ConsumerGroups.DELIVERY_SERVICE
    )
    @Transactional
    fun onDeliveryCommand(command: DeliveryCommand) {
        logger.info("Received delivery command for order ${command.orderId}: ${command::class.simpleName}")
        
        when (command) {
            is DeliveryCommand.ScheduleDelivery -> handleScheduleDelivery(command)
            is DeliveryCommand.CancelDelivery -> handleCancelDelivery(command)
        }
    }
    
    private fun handleScheduleDelivery(command: DeliveryCommand.ScheduleDelivery) {
        logger.info("Scheduling delivery for order ${command.orderId} to ${command.deliveryAddress}")
        
        // Create delivery
        val delivery = Delivery(
            orderId = command.orderId.value,
            deliveryAddress = command.deliveryAddress.value
        )
        
        // Simulate driver availability (98% success rate)
        val driverAvailable = simulateDriverAvailability()
        
        if (driverAvailable) {
            // Calculate estimated delivery time (pickup time + 30-45 min)
            val deliveryMinutes = (30..45).random()
            val estimatedDelivery = command.estimatedPickupTime.plusSeconds(deliveryMinutes * 60L)
            
            delivery.schedule(estimatedDelivery)
            deliveryRepository.save(delivery)
            
            logger.info("Delivery scheduled for order ${command.orderId}, driver: ${delivery.driverName}, ETA: $estimatedDelivery")
            
            // Send scheduled event
            val event = DeliveryEvent.DeliveryScheduled(
                orderId = command.orderId,
                deliveryId = DeliveryId(delivery.id),
                estimatedDeliveryTime = estimatedDelivery
            )
            kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, command.orderId.toString(), event)
            
            // Simulate delivery flow
            simulateDeliveryFlow(delivery, command.orderId)
        } else {
            val reason = "No drivers available in the area"
            delivery.fail(reason)
            deliveryRepository.save(delivery)
            
            logger.error("Delivery scheduling failed for order ${command.orderId}: $reason")
            
            val event = DeliveryEvent.DeliveryFailed(
                orderId = command.orderId,
                deliveryId = DeliveryId(delivery.id),
                reason = reason
            )
            kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, command.orderId.toString(), event)
        }
    }
    
    private fun handleCancelDelivery(command: DeliveryCommand.CancelDelivery) {
        logger.info("Cancelling delivery for order ${command.orderId}: ${command.reason}")
        
        val delivery = deliveryRepository.findByOrderId(command.orderId.value)
        if (delivery != null) {
            delivery.fail("Cancelled: ${command.reason}")
            deliveryRepository.save(delivery)
        }
    }
    
    private fun simulateDriverAvailability(): Boolean {
        return (Math.random() * 100) > 2 // 98% availability
    }
    
    private fun simulateDeliveryFlow(delivery: Delivery, orderId: OrderId) {
        // In a real system this would be scheduled tasks or state machine
        // For demo, we immediately complete the delivery
        
        // Pickup
        delivery.pickup()
        
        val pickupEvent = DeliveryEvent.DeliveryPickedUp(
            orderId = orderId,
            deliveryId = DeliveryId(delivery.id)
        )
        kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, orderId.toString(), pickupEvent)
        logger.info("Order $orderId picked up by driver ${delivery.driverName}")
        
        // Transit
        delivery.startTransit()
        
        // Complete
        delivery.complete()
        deliveryRepository.save(delivery)
        
        val completedEvent = DeliveryEvent.DeliveryCompleted(
            orderId = orderId,
            deliveryId = DeliveryId(delivery.id)
        )
        kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, orderId.toString(), completedEvent)
        logger.info("Order $orderId delivered successfully!")
    }
}
