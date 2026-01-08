package com.fooddelivery.kitchen.messaging

import com.fooddelivery.kitchen.domain.Ticket
import com.fooddelivery.kitchen.domain.TicketItem
import com.fooddelivery.kitchen.infrastructure.TicketRepository
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
class AvroKitchenCommandListener(
    private val ticketRepository: TicketRepository,
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {
    private val logger = LoggerFactory.getLogger(AvroKitchenCommandListener::class.java)
    
    @KafkaListener(
        topics = [KafkaTopics.KITCHEN_COMMANDS],
        groupId = "kitchen-service-avro",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    @Transactional
    fun onKitchenCommand(record: ConsumerRecord<String, ByteArray>) {
        try {
            val avroRecord = AvroSerializer.deserialize(record.value(), AvroSchemas.KITCHEN_COMMAND_SCHEMA)
            val command = KitchenAvroMapper.fromAvroCommand(avroRecord)
            
            logger.info("[AVRO] Received kitchen command for order ${command.orderId}: ${command::class.simpleName}")
            
            when (command) {
                is KitchenCommand.PrepareOrder -> handlePrepareOrder(command)
                is KitchenCommand.CancelTicket -> handleCancelTicket(command)
            }
        } catch (e: Exception) {
            logger.error("[AVRO] Error processing kitchen command: ${e.message}", e)
        }
    }
    
    private fun handlePrepareOrder(command: KitchenCommand.PrepareOrder) {
        logger.info("[AVRO] Processing PrepareOrder for ${command.orderId} with ${command.items.size} items")
        
        val ticket = Ticket(orderId = command.orderId.value)
        
        command.items.forEach { item ->
            ticket.items.add(
                TicketItem(
                    productId = item.productId.value,
                    productName = item.productName,
                    quantity = item.quantity
                )
            )
        }
        
        val canAccept = simulateKitchenAvailability()
        
        if (canAccept) {
            val estimatedMinutes = calculatePrepTime(command.items.size)
            ticket.accept(estimatedMinutes)
            ticketRepository.save(ticket)
            
            logger.info("[AVRO] Ticket accepted for order ${command.orderId}, ETA: $estimatedMinutes min")
            
            val event = KitchenEvent.TicketAccepted(
                orderId = command.orderId,
                ticketId = TicketId(ticket.id),
                estimatedPrepTimeMinutes = estimatedMinutes
            )
            val avroEvent = KitchenAvroMapper.toAvro(event)
            val bytes = AvroSerializer.serialize(avroEvent)
            kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, command.orderId.toString(), bytes)
            
            simulatePreparation(ticket, command.orderId)
        } else {
            val reason = "Kitchen at full capacity"
            ticket.reject(reason)
            ticketRepository.save(ticket)
            
            logger.warn("[AVRO] Ticket rejected for order ${command.orderId}: $reason")
            
            val event = KitchenEvent.TicketRejected(
                orderId = command.orderId,
                reason = reason
            )
            val avroEvent = KitchenAvroMapper.toAvro(event)
            val bytes = AvroSerializer.serialize(avroEvent)
            kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, command.orderId.toString(), bytes)
        }
    }
    
    private fun handleCancelTicket(command: KitchenCommand.CancelTicket) {
        logger.info("[AVRO] Cancelling ticket for order ${command.orderId}: ${command.reason}")
        
        val ticket = ticketRepository.findByOrderId(command.orderId.value)
        if (ticket != null) {
            ticket.reject("Cancelled: ${command.reason}")
            ticketRepository.save(ticket)
        }
    }
    
    private fun simulateKitchenAvailability(): Boolean {
        return (Math.random() * 100) > 5
    }
    
    private fun calculatePrepTime(itemCount: Int): Int {
        return 10 + (itemCount * 5)
    }
    
    private fun simulatePreparation(ticket: Ticket, orderId: OrderId) {
        ticket.markReady()
        ticketRepository.save(ticket)
        
        logger.info("[AVRO] Ticket ready for order $orderId")
        
        val event = KitchenEvent.TicketReady(
            orderId = orderId,
            ticketId = TicketId(ticket.id)
        )
        val avroEvent = KitchenAvroMapper.toAvro(event)
        val bytes = AvroSerializer.serialize(avroEvent)
        kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, orderId.toString(), bytes)
    }
}
