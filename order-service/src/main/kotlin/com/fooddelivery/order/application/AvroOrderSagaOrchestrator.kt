package com.fooddelivery.order.application

import com.fooddelivery.order.domain.*
import com.fooddelivery.order.infrastructure.client.*
import com.fooddelivery.order.infrastructure.messaging.AvroOrderEventPublisher
import com.fooddelivery.order.infrastructure.persistence.OrderRepository
import com.fooddelivery.shared.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AvroOrderSagaOrchestrator(
    private val orderRepository: OrderRepository,
    private val paymentClient: PaymentClient,
    private val eventPublisher: AvroOrderEventPublisher
) {
    private val logger = LoggerFactory.getLogger(AvroOrderSagaOrchestrator::class.java)
    
    @Transactional
    fun initiateSaga(request: CreateOrderRequest): Order {
        logger.info("[AVRO] Initiating saga for new order from customer ${request.customerId}")
        
        val order = createOrder(request)
        logger.info("[AVRO] Order ${order.id} created with status ${order.status}")
        
        eventPublisher.publishOrderCreated(
            orderId = order.orderId(),
            customerId = order.customerId(),
            totalAmount = MonetaryAmount(order.totalAmount)
        )
        
        val paymentResult = processPayment(order, request.paymentMethod)
        
        if (!paymentResult) {
            logger.warn("[AVRO] Payment failed for order ${order.id}, saga cancelled")
            return order
        }
        
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
                logger.info("[AVRO] Payment authorized for order ${order.id}, transaction: ${response.transactionId}")
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
        
        logger.info("[AVRO] Order ${order.id} sent to kitchen via Avro")
    }
    
    @Transactional
    fun onKitchenAccepted(orderId: OrderId, ticketId: TicketId, estimatedMinutes: Int) {
        val order = findOrder(orderId) ?: return
        
        order.ticketId = ticketId.value
        order.updateStatus(OrderStatus.DELIVERY_PENDING)
        orderRepository.save(order)
        
        val estimatedPickupTime = Instant.now().plusSeconds(estimatedMinutes * 60L)
        eventPublisher.sendScheduleDelivery(
            orderId = orderId,
            address = Address(order.deliveryAddress),
            estimatedPickupTime = estimatedPickupTime
        )
        
        logger.info("[AVRO] Order $orderId accepted by kitchen, scheduling delivery via Avro")
    }
    
    @Transactional
    fun onKitchenRejected(orderId: OrderId, reason: String) {
        val order = findOrder(orderId) ?: return
        
        logger.warn("[AVRO] Kitchen rejected order $orderId: $reason. Starting compensation...")
        
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
        
        logger.info("[AVRO] Delivery $deliveryId scheduled for order $orderId")
    }
    
    @Transactional
    fun onDeliveryCompleted(orderId: OrderId) {
        val order = findOrder(orderId) ?: return
        
        order.updateStatus(OrderStatus.COMPLETED)
        orderRepository.save(order)
        
        eventPublisher.publishOrderCompleted(orderId)
        
        logger.info("[AVRO] Order $orderId completed successfully!")
    }
    
    @Transactional
    fun onDeliveryFailed(orderId: OrderId, reason: String) {
        val order = findOrder(orderId) ?: return
        
        logger.error("[AVRO] Delivery failed for order $orderId: $reason. Starting compensation...")
        
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
            logger.error("[AVRO] Order $orderId not found!")
            null
        }
    }
}
