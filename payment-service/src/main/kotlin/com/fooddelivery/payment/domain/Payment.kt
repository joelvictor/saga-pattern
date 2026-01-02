package com.fooddelivery.payment.domain

import com.fooddelivery.shared.domain.PaymentMethod
import com.fooddelivery.shared.domain.PaymentStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments")
class Payment(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val orderId: UUID,
    
    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val paymentMethod: PaymentMethod,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,
    
    @Column
    var transactionId: String? = null,
    
    @Column
    var errorMessage: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column
    var processedAt: Instant? = null
) {
    fun authorize(): String {
        // Simulate payment gateway authorization
        val success = simulatePaymentGateway()
        
        if (success) {
            this.transactionId = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
            this.status = PaymentStatus.AUTHORIZED
        } else {
            this.status = PaymentStatus.REJECTED
            this.errorMessage = "Payment declined by issuer"
        }
        this.processedAt = Instant.now()
        
        return this.transactionId ?: ""
    }
    
    fun refund(reason: String): Boolean {
        if (this.status != PaymentStatus.AUTHORIZED) {
            return false
        }
        
        this.status = PaymentStatus.REFUNDED
        this.errorMessage = reason
        this.processedAt = Instant.now()
        return true
    }
    
    private fun simulatePaymentGateway(): Boolean {
        // Simulate 90% success rate
        return (Math.random() * 100) > 10
    }
}
