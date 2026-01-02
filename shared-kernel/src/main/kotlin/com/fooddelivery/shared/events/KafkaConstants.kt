package com.fooddelivery.shared.events

import com.fooddelivery.shared.domain.*

/**
 * Kafka topic constants for the food delivery system.
 */
object KafkaTopics {
    // Kitchen domain
    const val KITCHEN_COMMANDS = "kitchen.commands"
    const val KITCHEN_EVENTS = "kitchen.events"
    
    // Delivery domain
    const val DELIVERY_COMMANDS = "delivery.commands"
    const val DELIVERY_EVENTS = "delivery.events"
    
    // Order domain (for external notifications)
    const val ORDER_EVENTS = "order.events"
}

/**
 * Consumer group IDs.
 */
object ConsumerGroups {
    const val ORDER_SERVICE = "order-service"
    const val KITCHEN_SERVICE = "kitchen-service"
    const val DELIVERY_SERVICE = "delivery-service"
}
