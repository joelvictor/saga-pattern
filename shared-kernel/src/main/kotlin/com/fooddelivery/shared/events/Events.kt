package com.fooddelivery.shared.events

import com.fooddelivery.shared.domain.*
import java.time.Instant

/**
 * Base interface for all domain events.
 */
sealed interface DomainEvent {
    val eventId: String
    val timestamp: Instant
    val orderId: OrderId
}

// ============================================================
// KITCHEN EVENTS
// ============================================================

/**
 * Commands sent TO the Kitchen Service.
 */
sealed interface KitchenCommand {
    val orderId: OrderId
    
    data class PrepareOrder(
        override val orderId: OrderId,
        val items: List<OrderItem>,
        val priority: Int = 0,
        val timestamp: Instant = Instant.now()
    ) : KitchenCommand
    
    data class CancelTicket(
        override val orderId: OrderId,
        val reason: String,
        val timestamp: Instant = Instant.now()
    ) : KitchenCommand
}

/**
 * Events emitted BY the Kitchen Service.
 */
sealed interface KitchenEvent : DomainEvent {
    
    data class TicketAccepted(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val ticketId: TicketId,
        val estimatedPrepTimeMinutes: Int
    ) : KitchenEvent
    
    data class TicketRejected(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val reason: String
    ) : KitchenEvent
    
    data class TicketReady(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val ticketId: TicketId
    ) : KitchenEvent
}

// ============================================================
// DELIVERY EVENTS
// ============================================================

/**
 * Commands sent TO the Delivery Service.
 */
sealed interface DeliveryCommand {
    val orderId: OrderId
    
    data class ScheduleDelivery(
        override val orderId: OrderId,
        val deliveryAddress: Address,
        val estimatedPickupTime: Instant,
        val timestamp: Instant = Instant.now()
    ) : DeliveryCommand
    
    data class CancelDelivery(
        override val orderId: OrderId,
        val reason: String,
        val timestamp: Instant = Instant.now()
    ) : DeliveryCommand
}

/**
 * Events emitted BY the Delivery Service.
 */
sealed interface DeliveryEvent : DomainEvent {
    
    data class DeliveryScheduled(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val deliveryId: DeliveryId,
        val estimatedDeliveryTime: Instant
    ) : DeliveryEvent
    
    data class DeliveryPickedUp(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val deliveryId: DeliveryId
    ) : DeliveryEvent
    
    data class DeliveryCompleted(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val deliveryId: DeliveryId,
        val completedAt: Instant = Instant.now()
    ) : DeliveryEvent
    
    data class DeliveryFailed(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val deliveryId: DeliveryId,
        val reason: String
    ) : DeliveryEvent
}

// ============================================================
// ORDER EVENTS (for external notifications)
// ============================================================

/**
 * Events emitted BY the Order Service for external consumers.
 */
sealed interface OrderEvent : DomainEvent {
    
    data class OrderCreated(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val customerId: CustomerId,
        val totalAmount: MonetaryAmount
    ) : OrderEvent
    
    data class OrderCompleted(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val completedAt: Instant = Instant.now()
    ) : OrderEvent
    
    data class OrderCancelled(
        override val eventId: String = java.util.UUID.randomUUID().toString(),
        override val timestamp: Instant = Instant.now(),
        override val orderId: OrderId,
        val reason: String
    ) : OrderEvent
}
