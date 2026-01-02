package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.order.application.OrderSagaOrchestrator
import com.fooddelivery.shared.events.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka consumers for receiving events from Kitchen and Delivery services.
 */
@Component
class OrderEventConsumer(
    private val sagaOrchestrator: OrderSagaOrchestrator
) {
    private val logger = LoggerFactory.getLogger(OrderEventConsumer::class.java)
    
    /**
     * Handle events from Kitchen Service.
     */
    @KafkaListener(
        topics = [KafkaTopics.KITCHEN_EVENTS],
        groupId = ConsumerGroups.ORDER_SERVICE
    )
    fun onKitchenEvent(event: KitchenEvent) {
        logger.info("Received kitchen event for order ${event.orderId}: ${event::class.simpleName}")
        
        when (event) {
            is KitchenEvent.TicketAccepted -> {
                logger.info("Kitchen accepted order ${event.orderId}, ETA: ${event.estimatedPrepTimeMinutes} min")
                sagaOrchestrator.onKitchenAccepted(event.orderId, event.ticketId, event.estimatedPrepTimeMinutes)
            }
            is KitchenEvent.TicketRejected -> {
                logger.warn("Kitchen rejected order ${event.orderId}: ${event.reason}")
                sagaOrchestrator.onKitchenRejected(event.orderId, event.reason)
            }
            is KitchenEvent.TicketReady -> {
                logger.info("Kitchen ready for order ${event.orderId}")
                // Could trigger delivery pickup notification here
            }
        }
    }
    
    /**
     * Handle events from Delivery Service.
     */
    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_EVENTS],
        groupId = ConsumerGroups.ORDER_SERVICE
    )
    fun onDeliveryEvent(event: DeliveryEvent) {
        logger.info("Received delivery event for order ${event.orderId}: ${event::class.simpleName}")
        
        when (event) {
            is DeliveryEvent.DeliveryScheduled -> {
                logger.info("Delivery scheduled for order ${event.orderId}, ETA: ${event.estimatedDeliveryTime}")
                sagaOrchestrator.onDeliveryScheduled(event.orderId, event.deliveryId)
            }
            is DeliveryEvent.DeliveryPickedUp -> {
                logger.info("Delivery picked up for order ${event.orderId}")
            }
            is DeliveryEvent.DeliveryCompleted -> {
                logger.info("Delivery completed for order ${event.orderId}")
                sagaOrchestrator.onDeliveryCompleted(event.orderId)
            }
            is DeliveryEvent.DeliveryFailed -> {
                logger.error("Delivery failed for order ${event.orderId}: ${event.reason}")
                sagaOrchestrator.onDeliveryFailed(event.orderId, event.reason)
            }
        }
    }
}
