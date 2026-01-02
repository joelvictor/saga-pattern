package com.fooddelivery.shared.domain

/**
 * Payment method enumeration.
 */
enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PIX,
    WALLET
}

/**
 * Payment status from authorization response.
 */
enum class PaymentStatus {
    AUTHORIZED,
    REJECTED,
    PENDING,
    REFUNDED,
    FAILED
}

/**
 * Kitchen ticket status.
 */
enum class TicketStatus {
    PENDING,
    ACCEPTED,
    PREPARING,
    READY,
    REJECTED
}

/**
 * Delivery status tracking.
 */
enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED
}
