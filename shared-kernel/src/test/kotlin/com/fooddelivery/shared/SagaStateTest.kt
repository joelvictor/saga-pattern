package com.fooddelivery.shared

import com.fooddelivery.shared.saga.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SagaStateTest {

    @Test
    fun `should allow valid state transitions from Created`() {
        val from = SagaState.Created
        
        assertTrue(SagaState.isValidTransition(from, SagaState.PaymentPending))
        assertTrue(SagaState.isValidTransition(from, SagaState.Cancelled("reason")))
        assertFalse(SagaState.isValidTransition(from, SagaState.Paid))
        assertFalse(SagaState.isValidTransition(from, SagaState.Completed()))
    }

    @Test
    fun `should allow valid state transitions from PaymentPending`() {
        val from = SagaState.PaymentPending
        
        assertTrue(SagaState.isValidTransition(from, SagaState.Paid))
        assertTrue(SagaState.isValidTransition(from, SagaState.Cancelled("reason")))
        assertTrue(SagaState.isValidTransition(from, SagaState.Failed("error")))
        assertFalse(SagaState.isValidTransition(from, SagaState.Created))
    }

    @Test
    fun `should allow valid state transitions from Paid`() {
        val from = SagaState.Paid
        
        assertTrue(SagaState.isValidTransition(from, SagaState.KitchenPending))
        assertTrue(SagaState.isValidTransition(from, SagaState.Cancelled("reason")))
        assertFalse(SagaState.isValidTransition(from, SagaState.DeliveryPending))
    }

    @Test
    fun `should allow valid state transitions from KitchenPending`() {
        val from = SagaState.KitchenPending
        
        assertTrue(SagaState.isValidTransition(from, SagaState.DeliveryPending))
        assertTrue(SagaState.isValidTransition(from, SagaState.Cancelled("kitchen rejected")))
        assertFalse(SagaState.isValidTransition(from, SagaState.Paid))
    }

    @Test
    fun `should allow valid state transitions from DeliveryPending`() {
        val from = SagaState.DeliveryPending
        
        assertTrue(SagaState.isValidTransition(from, SagaState.Completed()))
        assertTrue(SagaState.isValidTransition(from, SagaState.Cancelled("cancelled")))
        assertTrue(SagaState.isValidTransition(from, SagaState.Failed("error")))
    }

    @Test
    fun `terminal states should not allow any transitions`() {
        val completed = SagaState.Completed()
        val cancelled = SagaState.Cancelled("reason")
        val failed = SagaState.Failed("error")
        
        // Terminal states should not transition to anything
        assertFalse(SagaState.isValidTransition(completed, SagaState.Created))
        assertFalse(SagaState.isValidTransition(completed, SagaState.Cancelled("x")))
        
        assertFalse(SagaState.isValidTransition(cancelled, SagaState.Created))
        assertFalse(SagaState.isValidTransition(cancelled, SagaState.Completed()))
        
        assertFalse(SagaState.isValidTransition(failed, SagaState.Created))
        assertFalse(SagaState.isValidTransition(failed, SagaState.Cancelled("x")))
    }

    @Test
    fun `should correctly identify terminal states`() {
        assertFalse(SagaState.Created.isTerminal())
        assertFalse(SagaState.PaymentPending.isTerminal())
        assertFalse(SagaState.Paid.isTerminal())
        assertFalse(SagaState.KitchenPending.isTerminal())
        assertFalse(SagaState.DeliveryPending.isTerminal())
        
        assertTrue(SagaState.Completed().isTerminal())
        assertTrue(SagaState.Cancelled("reason").isTerminal())
        assertTrue(SagaState.Failed("error").isTerminal())
    }

    @Test
    fun `should return correct display names`() {
        assertEquals("CREATED", SagaState.Created.displayName())
        assertEquals("PAYMENT_PENDING", SagaState.PaymentPending.displayName())
        assertEquals("PAID", SagaState.Paid.displayName())
        assertEquals("KITCHEN_PENDING", SagaState.KitchenPending.displayName())
        assertEquals("DELIVERY_PENDING", SagaState.DeliveryPending.displayName())
        assertEquals("COMPLETED", SagaState.Completed().displayName())
        assertEquals("CANCELLED", SagaState.Cancelled("x").displayName())
        assertEquals("FAILED", SagaState.Failed("x").displayName())
    }
}
