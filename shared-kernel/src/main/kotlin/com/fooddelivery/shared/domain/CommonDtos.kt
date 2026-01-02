package com.fooddelivery.shared.domain

import java.math.BigDecimal

/**
 * Order item representation.
 */
data class OrderItem(
    val productId: ProductId,
    val productName: String,
    val quantity: Int,
    val unitPrice: MonetaryAmount
) {
    val totalPrice: MonetaryAmount
        get() = unitPrice * quantity
}

/**
 * Order summary for cross-service communication.
 */
data class OrderSummary(
    val orderId: OrderId,
    val customerId: CustomerId,
    val items: List<OrderItem>,
    val totalAmount: MonetaryAmount,
    val deliveryAddress: Address
)
