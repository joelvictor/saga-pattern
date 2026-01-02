package com.fooddelivery.order

import com.fooddelivery.order.application.CreateOrderItemRequest
import com.fooddelivery.order.application.CreateOrderRequest
import com.fooddelivery.order.application.OrderSagaOrchestrator
import com.fooddelivery.order.domain.OrderStatus
import com.fooddelivery.order.infrastructure.client.PaymentAuthorizationResponse
import com.fooddelivery.order.infrastructure.client.PaymentClient
import com.fooddelivery.order.infrastructure.messaging.OrderEventPublisher
import com.fooddelivery.order.infrastructure.persistence.OrderRepository
import com.fooddelivery.shared.domain.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
class OrderSagaOrchestratorTest {

    @MockK
    private lateinit var orderRepository: OrderRepository

    @MockK
    private lateinit var paymentClient: PaymentClient

    @MockK(relaxed = true)
    private lateinit var eventPublisher: OrderEventPublisher

    private lateinit var orchestrator: OrderSagaOrchestrator

    @BeforeEach
    fun setup() {
        orchestrator = OrderSagaOrchestrator(orderRepository, paymentClient, eventPublisher)
        
        // Mock repository save to return the same order
        every { orderRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `should complete payment step when authorized`() {
        // Given
        val request = createTestOrderRequest()
        val paymentResponse = PaymentAuthorizationResponse(
            transactionId = "TXN-12345",
            status = PaymentStatus.AUTHORIZED,
            message = null
        )
        
        every { paymentClient.authorize(any()) } returns paymentResponse
        
        // When
        val order = orchestrator.initiateSaga(request)
        
        // Then
        assertEquals(OrderStatus.KITCHEN_PENDING, order.status)
        assertEquals("TXN-12345", order.transactionId)
        
        verify { eventPublisher.publishOrderCreated(any(), any(), any()) }
        verify { eventPublisher.sendPrepareOrder(any(), any(), any()) }
    }

    @Test
    fun `should cancel order when payment rejected`() {
        // Given
        val request = createTestOrderRequest()
        val paymentResponse = PaymentAuthorizationResponse(
            transactionId = null,
            status = PaymentStatus.REJECTED,
            message = "Insufficient funds"
        )
        
        every { paymentClient.authorize(any()) } returns paymentResponse
        
        // When
        val order = orchestrator.initiateSaga(request)
        
        // Then
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertNull(order.transactionId)
        assertNotNull(order.cancellationReason)
        
        verify { eventPublisher.publishOrderCreated(any(), any(), any()) }
        verify { eventPublisher.publishOrderCancelled(any(), any()) }
        verify(exactly = 0) { eventPublisher.sendPrepareOrder(any(), any(), any()) }
    }

    @Test
    fun `should compensate payment when kitchen rejects`() {
        // Given
        val orderId = OrderId(UUID.randomUUID())
        val order = mockk<com.fooddelivery.order.domain.Order>(relaxed = true)
        
        every { order.id } returns orderId.value
        every { order.transactionId } returns "TXN-12345"
        every { order.orderId() } returns orderId
        every { orderRepository.findById(orderId.value) } returns java.util.Optional.of(order)
        every { paymentClient.refund(any()) } returns mockk(relaxed = true)
        
        // When
        orchestrator.onKitchenRejected(orderId, "Kitchen at capacity")
        
        // Then
        verify { paymentClient.refund(any()) }
        verify { order.updateStatus(OrderStatus.CANCELLED, any()) }
        verify { eventPublisher.publishOrderCancelled(orderId, any()) }
    }

    @Test
    fun `should complete order when delivery completed`() {
        // Given
        val orderId = OrderId(UUID.randomUUID())
        val order = mockk<com.fooddelivery.order.domain.Order>(relaxed = true)
        
        every { order.orderId() } returns orderId
        every { orderRepository.findById(orderId.value) } returns java.util.Optional.of(order)
        
        // When
        orchestrator.onDeliveryCompleted(orderId)
        
        // Then
        verify { order.updateStatus(OrderStatus.COMPLETED) }
        verify { eventPublisher.publishOrderCompleted(orderId) }
    }

    private fun createTestOrderRequest() = CreateOrderRequest(
        customerId = UUID.randomUUID(),
        deliveryAddress = "Test Address 123",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        items = listOf(
            CreateOrderItemRequest(
                productId = ProductId("pizza-test"),
                productName = "Test Pizza",
                quantity = 2,
                unitPrice = MonetaryAmount.of(25.00)
            )
        )
    )
}
