package com.fooddelivery.shared.saga

import java.time.Instant

/**
 * Saga State Machine represented as a Sealed Interface.
 * Kotlin's exhaustive when expressions guarantee compile-time safety.
 */
sealed interface SagaState {
    
    /** Initial state when order is created */
    data object Created : SagaState
    
    /** Waiting for payment authorization */
    data object PaymentPending : SagaState
    
    /** Payment successfully authorized */
    data object Paid : SagaState
    
    /** Waiting for kitchen to accept/reject the order */
    data object KitchenPending : SagaState
    
    /** Kitchen accepted, waiting for delivery assignment */
    data object DeliveryPending : SagaState
    
    /** Order successfully completed */
    data class Completed(val completedAt: Instant = Instant.now()) : SagaState
    
    /** Order was cancelled (with reason for compensation tracking) */
    data class Cancelled(val reason: String, val cancelledAt: Instant = Instant.now()) : SagaState
    
    /** Order failed during processing */
    data class Failed(val error: String, val failedAt: Instant = Instant.now()) : SagaState
    
    companion object {
        /**
         * Validates if a state transition is allowed.
         */
        fun isValidTransition(from: SagaState, to: SagaState): Boolean = when (from) {
            is Created -> to is PaymentPending || to is Cancelled
            is PaymentPending -> to is Paid || to is Cancelled || to is Failed
            is Paid -> to is KitchenPending || to is Cancelled
            is KitchenPending -> to is DeliveryPending || to is Cancelled
            is DeliveryPending -> to is Completed || to is Cancelled || to is Failed
            is Completed -> false // Terminal state
            is Cancelled -> false // Terminal state
            is Failed -> false // Terminal state
        }
    }
}

/**
 * Extension function to get a human-readable name for logging.
 */
fun SagaState.displayName(): String = when (this) {
    is SagaState.Created -> "CREATED"
    is SagaState.PaymentPending -> "PAYMENT_PENDING"
    is SagaState.Paid -> "PAID"
    is SagaState.KitchenPending -> "KITCHEN_PENDING"
    is SagaState.DeliveryPending -> "DELIVERY_PENDING"
    is SagaState.Completed -> "COMPLETED"
    is SagaState.Cancelled -> "CANCELLED"
    is SagaState.Failed -> "FAILED"
}

/**
 * Extension to check if this is a terminal state.
 */
fun SagaState.isTerminal(): Boolean = this is SagaState.Completed || 
    this is SagaState.Cancelled || 
    this is SagaState.Failed
