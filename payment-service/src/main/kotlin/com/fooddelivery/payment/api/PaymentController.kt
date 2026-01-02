package com.fooddelivery.payment.api

import com.fooddelivery.payment.domain.Payment
import com.fooddelivery.payment.infrastructure.PaymentRepository
import com.fooddelivery.shared.domain.PaymentMethod
import com.fooddelivery.shared.domain.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentRepository: PaymentRepository
) {
    private val logger = LoggerFactory.getLogger(PaymentController::class.java)
    
    /**
     * Authorize a payment for an order.
     */
    @PostMapping("/authorize")
    @Transactional
    fun authorize(@RequestBody request: PaymentAuthorizationRequest): ResponseEntity<PaymentAuthorizationResponse> {
        logger.info("Authorizing payment for order ${request.orderId}, amount: ${request.amount}")
        
        // Check for existing payment
        val existingPayment = paymentRepository.findByOrderId(request.orderId)
        if (existingPayment != null && existingPayment.status == PaymentStatus.AUTHORIZED) {
            logger.warn("Payment already authorized for order ${request.orderId}")
            return ResponseEntity.ok(
                PaymentAuthorizationResponse(
                    transactionId = existingPayment.transactionId,
                    status = PaymentStatus.AUTHORIZED,
                    message = "Payment already authorized"
                )
            )
        }
        
        // Create and process new payment
        val payment = Payment(
            orderId = request.orderId,
            amount = request.amount,
            paymentMethod = request.paymentMethod
        )
        
        val transactionId = payment.authorize()
        paymentRepository.save(payment)
        
        logger.info("Payment ${payment.status} for order ${request.orderId}, transaction: $transactionId")
        
        return ResponseEntity.ok(
            PaymentAuthorizationResponse(
                transactionId = if (payment.status == PaymentStatus.AUTHORIZED) transactionId else null,
                status = payment.status,
                message = payment.errorMessage
            )
        )
    }
    
    /**
     * Refund a previously authorized payment.
     */
    @PostMapping("/refund")
    @Transactional
    fun refund(@RequestBody request: PaymentRefundRequest): ResponseEntity<PaymentRefundResponse> {
        logger.info("Processing refund for order ${request.orderId}, transaction: ${request.transactionId}")
        
        val payment = paymentRepository.findByTransactionId(request.transactionId)
            ?: return ResponseEntity.badRequest().body(
                PaymentRefundResponse(success = false, message = "Transaction not found")
            )
        
        val success = payment.refund(request.reason)
        paymentRepository.save(payment)
        
        logger.info("Refund ${if (success) "successful" else "failed"} for order ${request.orderId}")
        
        return ResponseEntity.ok(
            PaymentRefundResponse(
                success = success,
                message = if (success) "Refund processed" else "Refund failed: invalid payment status"
            )
        )
    }
    
    /**
     * Get payment by order ID.
     */
    @GetMapping("/order/{orderId}")
    fun getByOrderId(@PathVariable orderId: UUID): ResponseEntity<Payment> {
        return paymentRepository.findByOrderId(orderId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}

// DTOs
data class PaymentAuthorizationRequest(
    val orderId: UUID,
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod
)

data class PaymentAuthorizationResponse(
    val transactionId: String?,
    val status: PaymentStatus,
    val message: String? = null
)

data class PaymentRefundRequest(
    val orderId: UUID,
    val transactionId: String,
    val reason: String
)

data class PaymentRefundResponse(
    val success: Boolean,
    val message: String? = null
)
