package com.fooddelivery.order.api

import com.fooddelivery.order.application.CreateOrderItemRequest
import com.fooddelivery.order.application.CreateOrderRequest
import com.fooddelivery.order.application.OrderSagaOrchestrator
import com.fooddelivery.order.domain.Order
import com.fooddelivery.order.domain.OrderStatus
import com.fooddelivery.order.infrastructure.persistence.OrderRepository
import com.fooddelivery.shared.domain.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST Controller for Order operations.
 * Uses Virtual Threads - no need for suspend functions or reactive types.
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val sagaOrchestrator: OrderSagaOrchestrator,
    private val orderRepository: OrderRepository
) {
    
    /**
     * Create a new order and initiate the saga.
     */
    @PostMapping
    fun createOrder(@RequestBody request: OrderRequest): ResponseEntity<OrderResponse> {
        val createRequest = CreateOrderRequest(
            customerId = request.customerId,
            deliveryAddress = request.deliveryAddress,
            paymentMethod = request.paymentMethod,
            items = request.items.map { item ->
                CreateOrderItemRequest(
                    productId = ProductId(item.productId),
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = MonetaryAmount.of(item.unitPrice)
                )
            }
        )
        
        val order = sagaOrchestrator.initiateSaga(createRequest)
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(order.toResponse())
    }
    
    /**
     * Get order by ID.
     */
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> {
        return orderRepository.findById(orderId)
            .map { ResponseEntity.ok(it.toResponse()) }
            .orElse(ResponseEntity.notFound().build())
    }
    
    /**
     * Get orders by customer.
     */
    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: UUID): ResponseEntity<List<OrderResponse>> {
        val orders = orderRepository.findByCustomerId(customerId)
        return ResponseEntity.ok(orders.map { it.toResponse() })
    }
    
    /**
     * Get orders by status.
     */
    @GetMapping("/status/{status}")
    fun getOrdersByStatus(@PathVariable status: OrderStatus): ResponseEntity<List<OrderResponse>> {
        val orders = orderRepository.findByStatus(status)
        return ResponseEntity.ok(orders.map { it.toResponse() })
    }
    
    /**
     * Get all active (non-terminal) orders.
     */
    @GetMapping("/active")
    fun getActiveOrders(): ResponseEntity<List<OrderResponse>> {
        val orders = orderRepository.findActiveOrders()
        return ResponseEntity.ok(orders.map { it.toResponse() })
    }
}

// ========================================================
// API DTOs
// ========================================================

data class OrderRequest(
    val customerId: UUID,
    val deliveryAddress: String,
    val paymentMethod: PaymentMethod,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
)

data class OrderResponse(
    val id: UUID,
    val customerId: UUID,
    val deliveryAddress: String,
    val totalAmount: Double,
    val status: OrderStatus,
    val transactionId: String?,
    val ticketId: UUID?,
    val deliveryId: UUID?,
    val cancellationReason: String?,
    val items: List<OrderItemResponse>,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?
)

data class OrderItemResponse(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

// Extension function for Order -> OrderResponse conversion
fun Order.toResponse() = OrderResponse(
    id = this.id,
    customerId = this.customerId,
    deliveryAddress = this.deliveryAddress,
    totalAmount = this.totalAmount.toDouble(),
    status = this.status,
    transactionId = this.transactionId,
    ticketId = this.ticketId,
    deliveryId = this.deliveryId,
    cancellationReason = this.cancellationReason,
    items = this.items.map { item ->
        OrderItemResponse(
            productId = item.productId,
            productName = item.productName,
            quantity = item.quantity,
            unitPrice = item.unitPrice.toDouble(),
            totalPrice = (item.unitPrice * item.quantity.toBigDecimal()).toDouble()
        )
    },
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString(),
    completedAt = this.completedAt?.toString()
)
