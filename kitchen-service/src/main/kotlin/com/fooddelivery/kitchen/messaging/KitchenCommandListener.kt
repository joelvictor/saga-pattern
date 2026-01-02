package com.fooddelivery.kitchen.messaging

import com.fooddelivery.kitchen.domain.Ticket
import com.fooddelivery.kitchen.domain.TicketItem
import com.fooddelivery.kitchen.infrastructure.TicketRepository
import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class KitchenCommandListener(
    private val ticketRepository: TicketRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(KitchenCommandListener::class.java)
    
    @KafkaListener(
        topics = [KafkaTopics.KITCHEN_COMMANDS],
        groupId = ConsumerGroups.KITCHEN_SERVICE
    )
    @Transactional
    fun onKitchenCommand(command: KitchenCommand) {
        logger.info("Received kitchen command for order ${command.orderId}: ${command::class.simpleName}")
        
        when (command) {
            is KitchenCommand.PrepareOrder -> handlePrepareOrder(command)
            is KitchenCommand.CancelTicket -> handleCancelTicket(command)
        }
    }
    
    private fun handlePrepareOrder(command: KitchenCommand.PrepareOrder) {
        logger.info("Processing PrepareOrder for ${command.orderId} with ${command.items.size} items")
        
        // Create ticket
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
        
        // Simulate kitchen availability check (95% acceptance rate)
        val canAccept = simulateKitchenAvailability()
        
        if (canAccept) {
            val estimatedMinutes = calculatePrepTime(command.items.size)
            ticket.accept(estimatedMinutes)
            ticketRepository.save(ticket)
            
            logger.info("Ticket accepted for order ${command.orderId}, ETA: $estimatedMinutes min")
            
            // Send accepted event
            val event = KitchenEvent.TicketAccepted(
                orderId = command.orderId,
                ticketId = TicketId(ticket.id),
                estimatedPrepTimeMinutes = estimatedMinutes
            )
            kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, command.orderId.toString(), event)
            
            // Simulate preparation (in real scenario, this would be async)
            simulatePreparation(ticket, command.orderId)
        } else {
            val reason = "Kitchen at full capacity"
            ticket.reject(reason)
            ticketRepository.save(ticket)
            
            logger.warn("Ticket rejected for order ${command.orderId}: $reason")
            
            // Send rejected event
            val event = KitchenEvent.TicketRejected(
                orderId = command.orderId,
                reason = reason
            )
            kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, command.orderId.toString(), event)
        }
    }
    
    private fun handleCancelTicket(command: KitchenCommand.CancelTicket) {
        logger.info("Cancelling ticket for order ${command.orderId}: ${command.reason}")
        
        val ticket = ticketRepository.findByOrderId(command.orderId.value)
        if (ticket != null) {
            ticket.reject("Cancelled: ${command.reason}")
            ticketRepository.save(ticket)
        }
    }
    
    private fun simulateKitchenAvailability(): Boolean {
        return (Math.random() * 100) > 5 // 95% acceptance rate
    }
    
    private fun calculatePrepTime(itemCount: Int): Int {
        // Base time + additional time per item
        return 10 + (itemCount * 5)
    }
    
    private fun simulatePreparation(ticket: Ticket, orderId: OrderId) {
        // In a real system, this would be handled by a scheduler or state machine
        // For demo, we immediately mark as ready
        ticket.markReady()
        ticketRepository.save(ticket)
        
        logger.info("Ticket ready for order $orderId")
        
        val event = KitchenEvent.TicketReady(
            orderId = orderId,
            ticketId = TicketId(ticket.id)
        )
        kafkaTemplate.send(KafkaTopics.KITCHEN_EVENTS, orderId.toString(), event)
    }
}
