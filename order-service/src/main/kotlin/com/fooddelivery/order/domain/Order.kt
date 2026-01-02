package com.fooddelivery.order.domain

import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.saga.SagaState
import com.fooddelivery.shared.saga.displayName
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Order aggregate root entity.
 */
@Entity
@Table(name = "orders")
class Order(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val customerId: UUID,
    
    @Column(nullable = false)
    val deliveryAddress: String,
    
    @Column(nullable = false, precision = 10, scale = 2)
    val totalAmount: java.math.BigDecimal,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED,
    
    @Column
    var transactionId: String? = null,
    
    @Column
    var ticketId: UUID? = null,
    
    @Column
    var deliveryId: UUID? = null,
    
    @Column
    var cancellationReason: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column
    var updatedAt: Instant = Instant.now(),
    
    @Column
    var completedAt: Instant? = null,
    
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    val items: MutableList<OrderItemEntity> = mutableListOf()
) {
    // Domain IDs (Value Class wrappers)
    fun orderId(): OrderId = OrderId(id)
    fun customerId(): CustomerId = CustomerId(customerId)
    
    /**
     * Update order status with timestamp.
     */
    fun updateStatus(newStatus: OrderStatus, reason: String? = null) {
        this.status = newStatus
        this.updatedAt = Instant.now()
        if (newStatus == OrderStatus.CANCELLED) {
            this.cancellationReason = reason
        }
        if (newStatus == OrderStatus.COMPLETED) {
            this.completedAt = Instant.now()
        }
    }
    
    /**
     * Convert to order summary for messaging.
     */
    fun toSummary(): OrderSummary = OrderSummary(
        orderId = orderId(),
        customerId = customerId(),
        items = items.map { it.toOrderItem() },
        totalAmount = MonetaryAmount(totalAmount),
        deliveryAddress = Address(deliveryAddress)
    )
}

/**
 * Order item entity for JPA persistence.
 */
@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val productId: String,
    
    @Column(nullable = false)
    val productName: String,
    
    @Column(nullable = false)
    val quantity: Int,
    
    @Column(nullable = false, precision = 10, scale = 2)
    val unitPrice: java.math.BigDecimal
) {
    fun toOrderItem(): OrderItem = OrderItem(
        productId = ProductId(productId),
        productName = productName,
        quantity = quantity,
        unitPrice = MonetaryAmount(unitPrice)
    )
}

/**
 * Order status enum matching saga states.
 */
enum class OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    KITCHEN_PENDING,
    DELIVERY_PENDING,
    COMPLETED,
    CANCELLED,
    FAILED;
    
    fun toSagaState(): SagaState = when (this) {
        CREATED -> SagaState.Created
        PAYMENT_PENDING -> SagaState.PaymentPending
        PAID -> SagaState.Paid
        KITCHEN_PENDING -> SagaState.KitchenPending
        DELIVERY_PENDING -> SagaState.DeliveryPending
        COMPLETED -> SagaState.Completed()
        CANCELLED -> SagaState.Cancelled("Unknown")
        FAILED -> SagaState.Failed("Unknown")
    }
}
