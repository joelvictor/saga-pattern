package com.fooddelivery.delivery.messaging

import com.fooddelivery.delivery.domain.Delivery
import com.fooddelivery.delivery.infrastructure.DeliveryRepository
import com.fooddelivery.shared.avro.*
import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AvroDeliveryCommandListener(
    private val deliveryRepository: DeliveryRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {
    private val logger = LoggerFactory.getLogger(AvroDeliveryCommandListener::class.java)
    
    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_COMMANDS],
        groupId = "delivery-service-avro",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    @Transactional
    fun onDeliveryCommand(record: ConsumerRecord<String, ByteArray>) {
        try {
            val avroRecord = AvroSerializer.deserialize(record.value(), AvroSchemas.DELIVERY_COMMAND_SCHEMA)
            val command = DeliveryAvroMapper.fromAvroCommand(avroRecord)
            
            logger.info("[AVRO] Received delivery command for order ${command.orderId}: ${command::class.simpleName}")
            
            when (command) {
                is DeliveryCommand.ScheduleDelivery -> handleScheduleDelivery(command)
                is DeliveryCommand.CancelDelivery -> handleCancelDelivery(command)
            }
        } catch (e: Exception) {
            logger.error("[AVRO] Error processing delivery command: ${e.message}", e)
        }
    }
    
    private fun handleScheduleDelivery(command: DeliveryCommand.ScheduleDelivery) {
        logger.info("[AVRO] Scheduling delivery for order ${command.orderId} to ${command.deliveryAddress}")
        
        val delivery = Delivery(
            orderId = command.orderId.value,
            deliveryAddress = command.deliveryAddress.value
        )
        
        val driverAvailable = simulateDriverAvailability()
        
        if (driverAvailable) {
            val deliveryMinutes = (30..45).random()
            val estimatedDelivery = command.estimatedPickupTime.plusSeconds(deliveryMinutes * 60L)
            
            delivery.schedule(estimatedDelivery)
            deliveryRepository.save(delivery)
            
            logger.info("[AVRO] Delivery scheduled for order ${command.orderId}, driver: ${delivery.driverName}, ETA: $estimatedDelivery")
            
            val event = DeliveryEvent.DeliveryScheduled(
                orderId = command.orderId,
                deliveryId = DeliveryId(delivery.id),
                estimatedDeliveryTime = estimatedDelivery
            )
            val avroEvent = DeliveryAvroMapper.toAvro(event)
            val bytes = AvroSerializer.serialize(avroEvent)
            kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, command.orderId.toString(), bytes)
            
            simulateDeliveryFlow(delivery, command.orderId)
        } else {
            val reason = "No drivers available in the area"
            delivery.fail(reason)
            deliveryRepository.save(delivery)
            
            logger.error("[AVRO] Delivery scheduling failed for order ${command.orderId}: $reason")
            
            val event = DeliveryEvent.DeliveryFailed(
                orderId = command.orderId,
                deliveryId = DeliveryId(delivery.id),
                reason = reason
            )
            val avroEvent = DeliveryAvroMapper.toAvro(event)
            val bytes = AvroSerializer.serialize(avroEvent)
            kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, command.orderId.toString(), bytes)
        }
    }
    
    private fun handleCancelDelivery(command: DeliveryCommand.CancelDelivery) {
        logger.info("[AVRO] Cancelling delivery for order ${command.orderId}: ${command.reason}")
        
        val delivery = deliveryRepository.findByOrderId(command.orderId.value)
        if (delivery != null) {
            delivery.fail("Cancelled: ${command.reason}")
            deliveryRepository.save(delivery)
        }
    }
    
    private fun simulateDriverAvailability(): Boolean {
        return (Math.random() * 100) > 2
    }
    
    private fun simulateDeliveryFlow(delivery: Delivery, orderId: OrderId) {
        delivery.pickup()
        
        val pickupEvent = DeliveryEvent.DeliveryPickedUp(
            orderId = orderId,
            deliveryId = DeliveryId(delivery.id)
        )
        val avroPickupEvent = DeliveryAvroMapper.toAvro(pickupEvent)
        val pickupBytes = AvroSerializer.serialize(avroPickupEvent)
        kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, orderId.toString(), pickupBytes)
        logger.info("[AVRO] Order $orderId picked up by driver ${delivery.driverName}")
        
        delivery.startTransit()
        
        delivery.complete()
        deliveryRepository.save(delivery)
        
        val completedEvent = DeliveryEvent.DeliveryCompleted(
            orderId = orderId,
            deliveryId = DeliveryId(delivery.id)
        )
        val avroCompletedEvent = DeliveryAvroMapper.toAvro(completedEvent)
        val completedBytes = AvroSerializer.serialize(avroCompletedEvent)
        kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, orderId.toString(), completedBytes)
        logger.info("[AVRO] Order $orderId delivered successfully!")
    }
}
