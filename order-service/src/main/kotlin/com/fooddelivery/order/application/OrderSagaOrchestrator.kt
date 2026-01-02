package com.fooddelivery.order.application

import com.fooddelivery.order.domain.*
import com.fooddelivery.order.infrastructure.client.*
import com.fooddelivery.order.infrastructure.messaging.OrderEventPublisher
import com.fooddelivery.order.infrastructure.persistence.OrderRepository
import com.fooddelivery.shared.domain.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Saga Orchestrator for Order processing.
 * Uses Kotlin Coroutines running on Java 25 Virtual Threads.
 */
@Service
class OrderSagaOrchestrator(
    private val orderRepository: OrderRepository,
    private val paymentClient: PaymentClient,
    private val eventPublisher: OrderEventPublisher
) {
    private val logger = LoggerFactory.getLogger(OrderSagaOrchestrator::class.java)
    
    /**
     * Initiate a new order saga.
     * This is the main entry point - runs synchronously on Virtual Thread.
     */
    @Transactional
    fun initiateSaga(request: CreateOrderRequest): Order {
        logger.info("Initiating saga for new order from customer ${request.customerId}")
        
        // Step 1: Create order
        val order = createOrder(request)
        logger.info("Order ${order.id} created with status ${order.status}")
        
        // Publish order created event
        eventPublisher.publishOrderCreated(
            orderId = order.orderId(),
            customerId = order.customerId(),
            totalAmount = MonetaryAmount(order.totalAmount)
        )
        
        // Step 2: Process payment (synchronous - runs on Virtual Thread)
        val paymentResult = processPayment(order, request.paymentMethod)
        
        if (!paymentResult) {
            logger.warn("Payment failed for order ${order.id}, saga cancelled")
            return order
        }
        
        // Step 3: Send to kitchen (async via Kafka)
        sendToKitchen(order)
        
        return order
    }
    
    private fun createOrder(request: CreateOrderRequest): Order {
        val order = Order(
            customerId = request.customerId,
            deliveryAddress = request.deliveryAddress,
            totalAmount = request.items.sumOf { it.unitPrice.value * it.quantity.toBigDecimal() }
        )
        
        request.items.forEach { item ->
            order.items.add(
                OrderItemEntity(
                    productId = item.productId.value,
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice.value
                )
            )
        }
        
        return orderRepository.save(order)
    }
    
    private fun processPayment(order: Order, paymentMethod: PaymentMethod): Boolean {
        order.updateStatus(OrderStatus.PAYMENT_PENDING)
        orderRepository.save(order)
        
        val paymentRequest = PaymentAuthorizationRequest(
            orderId = order.id,
            amount = order.totalAmount,
            paymentMethod = paymentMethod
        )
        
        val response = paymentClient.authorize(paymentRequest)
        
        return when (response.status) {
            PaymentStatus.AUTHORIZED -> {
                order.transactionId = response.transactionId
                order.updateStatus(OrderStatus.PAID)
                orderRepository.save(order)
                logger.info("Payment authorized for order ${order.id}, transaction: ${response.transactionId}")
                true
            }
            else -> {
                order.updateStatus(OrderStatus.CANCELLED, "Payment ${response.status}: ${response.message}")
                orderRepository.save(order)
                eventPublisher.publishOrderCancelled(order.orderId(), "Payment rejected")
                false
            }
        }
    }
    
    private fun sendToKitchen(order: Order) {
        order.updateStatus(OrderStatus.KITCHEN_PENDING)
        orderRepository.save(order)
        
        eventPublisher.sendPrepareOrder(
            orderId = order.orderId(),
            items = order.items.map { it.toOrderItem() }
        )
        
        logger.info("Order ${order.id} sent to kitchen")
    }
    
    // ========================================================
    // EVENT HANDLERS (called by Kafka consumers)
    // ========================================================
    
    @Transactional
    fun onKitchenAccepted(orderId: OrderId, ticketId: TicketId, estimatedMinutes: Int) {
        val order = findOrder(orderId) ?: return
        
        order.ticketId = ticketId.value
        order.updateStatus(OrderStatus.DELIVERY_PENDING)
        orderRepository.save(order)
        
        // Schedule delivery
        val estimatedPickupTime = Instant.now().plusSeconds(estimatedMinutes * 60L)
        eventPublisher.sendScheduleDelivery(
            orderId = orderId,
            address = Address(order.deliveryAddress),
            estimatedPickupTime = estimatedPickupTime
        )
        
        logger.info("Order $orderId accepted by kitchen, scheduling delivery")
    }
    
    @Transactional
    fun onKitchenRejected(orderId: OrderId, reason: String) {
        val order = findOrder(orderId) ?: return
        
        logger.warn("Kitchen rejected order $orderId: $reason. Starting compensation...")
        
        // Compensate: Refund payment
        order.transactionId?.let { transactionId ->
            val refundRequest = PaymentRefundRequest(
                orderId = order.id,
                transactionId = transactionId,
                reason = "Kitchen rejected: $reason"
            )
            paymentClient.refund(refundRequest)
        }
        
        order.updateStatus(OrderStatus.CANCELLED, "Kitchen rejected: $reason")
        orderRepository.save(order)
        
        eventPublisher.publishOrderCancelled(orderId, reason)
    }
    
    @Transactional
    fun onDeliveryScheduled(orderId: OrderId, deliveryId: DeliveryId) {
        val order = findOrder(orderId) ?: return
        
        order.deliveryId = deliveryId.value
        orderRepository.save(order)
        
        logger.info("Delivery $deliveryId scheduled for order $orderId")
    }
    
    @Transactional
    fun onDeliveryCompleted(orderId: OrderId) {
        val order = findOrder(orderId) ?: return
        
        order.updateStatus(OrderStatus.COMPLETED)
        orderRepository.save(order)
        
        eventPublisher.publishOrderCompleted(orderId)
        
        logger.info("Order $orderId completed successfully!")
    }
    
    @Transactional
    fun onDeliveryFailed(orderId: OrderId, reason: String) {
        val order = findOrder(orderId) ?: return
        
        logger.error("Delivery failed for order $orderId: $reason. Starting compensation...")
        
        // Compensate: Refund payment
        order.transactionId?.let { transactionId ->
            val refundRequest = PaymentRefundRequest(
                orderId = order.id,
                transactionId = transactionId,
                reason = "Delivery failed: $reason"
            )
            paymentClient.refund(refundRequest)
        }
        
        order.updateStatus(OrderStatus.FAILED, "Delivery failed: $reason")
        orderRepository.save(order)
        
        eventPublisher.publishOrderCancelled(orderId, reason)
    }
    
    private fun findOrder(orderId: OrderId): Order? {
        return orderRepository.findById(orderId.value).orElseGet {
            logger.error("Order $orderId not found!")
            null
        }
    }
}

// DTO for creating orders
data class CreateOrderRequest(
    val customerId: UUID,
    val deliveryAddress: String,
    val paymentMethod: PaymentMethod,
    val items: List<CreateOrderItemRequest>
)

data class CreateOrderItemRequest(
    val productId: ProductId,
    val productName: String,
    val quantity: Int,
    val unitPrice: MonetaryAmount
)
