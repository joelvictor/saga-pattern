package com.fooddelivery.shared.domain

import java.math.BigDecimal
import java.util.UUID

/**
 * Domain Primitives using Kotlin Value Classes (Project Valhalla compatible).
 * These provide type-safe wrappers without runtime overhead.
 */

@JvmInline
value class OrderId(val value: UUID) {
    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID())
        fun fromString(value: String): OrderId = OrderId(UUID.fromString(value))
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
value class CustomerId(val value: UUID) {
    companion object {
        fun generate(): CustomerId = CustomerId(UUID.randomUUID())
        fun fromString(value: String): CustomerId = CustomerId(UUID.fromString(value))
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "TransactionId cannot be blank" }
    }
    
    companion object {
        fun generate(): TransactionId = TransactionId(UUID.randomUUID().toString())
    }
    
    override fun toString(): String = value
}

@JvmInline
value class TicketId(val value: UUID) {
    companion object {
        fun generate(): TicketId = TicketId(UUID.randomUUID())
        fun fromString(value: String): TicketId = TicketId(UUID.fromString(value))
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
value class DeliveryId(val value: UUID) {
    companion object {
        fun generate(): DeliveryId = DeliveryId(UUID.randomUUID())
        fun fromString(value: String): DeliveryId = DeliveryId(UUID.fromString(value))
    }
    
    override fun toString(): String = value.toString()
}

@JvmInline
value class MonetaryAmount(val value: BigDecimal) {
    init {
        require(value >= BigDecimal.ZERO) { "MonetaryAmount cannot be negative" }
    }
    
    operator fun plus(other: MonetaryAmount): MonetaryAmount = 
        MonetaryAmount(value + other.value)
    
    operator fun minus(other: MonetaryAmount): MonetaryAmount = 
        MonetaryAmount(value - other.value)
    
    operator fun times(quantity: Int): MonetaryAmount = 
        MonetaryAmount(value * quantity.toBigDecimal())
    
    companion object {
        val ZERO = MonetaryAmount(BigDecimal.ZERO)
        fun of(amount: Double): MonetaryAmount = MonetaryAmount(BigDecimal.valueOf(amount))
        fun of(amount: String): MonetaryAmount = MonetaryAmount(BigDecimal(amount))
    }
    
    override fun toString(): String = value.setScale(2).toString()
}

@JvmInline
value class ProductId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProductId cannot be blank" }
    }
    
    override fun toString(): String = value
}

@JvmInline
value class Address(val value: String) {
    init {
        require(value.isNotBlank()) { "Address cannot be blank" }
    }
    
    override fun toString(): String = value
}
