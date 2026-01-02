package com.fooddelivery.order.infrastructure.client

import com.fooddelivery.shared.domain.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

/**
 * REST client for Payment Service using Spring's RestClient (Virtual Threads compatible).
 */
@Component
class PaymentClient(
    @Value("\${payment.service.url}") 
    private val paymentServiceUrl: String
) {
    private val logger = LoggerFactory.getLogger(PaymentClient::class.java)
    
    private val restClient = RestClient.builder()
        .baseUrl(paymentServiceUrl)
        .build()
    
    /**
     * Authorize payment for an order.
     * Runs on Virtual Thread - no need for reactive types.
     */
    fun authorize(request: PaymentAuthorizationRequest): PaymentAuthorizationResponse {
        logger.info("Authorizing payment for order ${request.orderId}, amount: ${request.amount}")
        
        return try {
            val response = restClient.post()
                .uri("/api/v1/payments/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PaymentAuthorizationResponse::class.java)
                ?: throw PaymentException("Empty response from payment service")
            
            logger.info("Payment authorization result for order ${request.orderId}: ${response.status}")
            response
        } catch (e: Exception) {
            logger.error("Payment authorization failed for order ${request.orderId}", e)
            PaymentAuthorizationResponse(
                transactionId = null,
                status = PaymentStatus.FAILED,
                message = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Refund a previously authorized payment.
     */
    fun refund(request: PaymentRefundRequest): PaymentRefundResponse {
        logger.info("Refunding payment for order ${request.orderId}, transaction: ${request.transactionId}")
        
        return try {
            val response = restClient.post()
                .uri("/api/v1/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PaymentRefundResponse::class.java)
                ?: throw PaymentException("Empty response from payment service")
            
            logger.info("Refund result for order ${request.orderId}: ${if (response.success) "SUCCESS" else "FAILED"}")
            response
        } catch (e: Exception) {
            logger.error("Refund failed for order ${request.orderId}", e)
            PaymentRefundResponse(
                success = false,
                message = e.message ?: "Unknown error"
            )
        }
    }
}

// DTOs for Payment Service communication
data class PaymentAuthorizationRequest(
    val orderId: UUID,
    val amount: java.math.BigDecimal,
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

class PaymentException(message: String) : RuntimeException(message)
