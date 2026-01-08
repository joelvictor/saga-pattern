package com.fooddelivery.order.infrastructure.messaging

import com.fooddelivery.order.application.AvroOrderSagaOrchestrator
import com.fooddelivery.shared.avro.*
import com.fooddelivery.shared.events.KafkaTopics
import com.fooddelivery.shared.events.DeliveryEvent
import com.fooddelivery.shared.events.KitchenEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class AvroOrderEventConsumer(
    private val sagaOrchestrator: AvroOrderSagaOrchestrator
) {
    private val logger = LoggerFactory.getLogger(AvroOrderEventConsumer::class.java)
    
    @KafkaListener(
        topics = [KafkaTopics.KITCHEN_EVENTS],
        groupId = "order-service-avro",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    fun handleKitchenEvent(record: ConsumerRecord<String, ByteArray>) {
        try {
            val avroRecord = AvroSerializer.deserialize(record.value(), AvroSchemas.KITCHEN_EVENT_SCHEMA)
            val event = KitchenAvroMapper.fromAvroEvent(avroRecord)
            
            logger.info("[AVRO] Received kitchen event: ${event::class.simpleName} for order ${event.orderId}")
            
            when (event) {
                is KitchenEvent.TicketAccepted -> {
                    sagaOrchestrator.onKitchenAccepted(
                        orderId = event.orderId,
                        ticketId = event.ticketId,
                        estimatedMinutes = event.estimatedPrepTimeMinutes
                    )
                }
                is KitchenEvent.TicketRejected -> {
                    sagaOrchestrator.onKitchenRejected(
                        orderId = event.orderId,
                        reason = event.reason
                    )
                }
                is KitchenEvent.TicketReady -> {
                    logger.info("[AVRO] Ticket ready for order ${event.orderId}")
                }
            }
        } catch (e: Exception) {
            logger.error("[AVRO] Error processing kitchen event: ${e.message}", e)
        }
    }
    
    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_EVENTS],
        groupId = "order-service-avro",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    fun handleDeliveryEvent(record: ConsumerRecord<String, ByteArray>) {
        try {
            val avroRecord = AvroSerializer.deserialize(record.value(), AvroSchemas.DELIVERY_EVENT_SCHEMA)
            val event = DeliveryAvroMapper.fromAvroEvent(avroRecord)
            
            logger.info("[AVRO] Received delivery event: ${event::class.simpleName} for order ${event.orderId}")
            
            when (event) {
                is DeliveryEvent.DeliveryScheduled -> {
                    sagaOrchestrator.onDeliveryScheduled(
                        orderId = event.orderId,
                        deliveryId = event.deliveryId
                    )
                }
                is DeliveryEvent.DeliveryCompleted -> {
                    sagaOrchestrator.onDeliveryCompleted(orderId = event.orderId)
                }
                is DeliveryEvent.DeliveryFailed -> {
                    sagaOrchestrator.onDeliveryFailed(
                        orderId = event.orderId,
                        reason = event.reason
                    )
                }
                is DeliveryEvent.DeliveryPickedUp -> {
                    logger.info("[AVRO] Delivery picked up for order ${event.orderId}")
                }
            }
        } catch (e: Exception) {
            logger.error("[AVRO] Error processing delivery event: ${e.message}", e)
        }
    }
}
