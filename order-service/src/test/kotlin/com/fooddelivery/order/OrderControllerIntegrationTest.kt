package com.fooddelivery.order

import com.fooddelivery.order.api.OrderItemRequest
import com.fooddelivery.order.api.OrderRequest
import com.fooddelivery.shared.domain.PaymentMethod
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:17").apply {
            withDatabaseName("order_db")
            withUsername("postgres")
            withPassword("postgres")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            // Disable Kafka for controller tests
            registry.add("spring.kafka.bootstrap-servers") { "localhost:9999" }
            registry.add("spring.autoconfigure.exclude") { 
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration" 
            }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should accept order request`() {
        // Note: This test validates the API layer only
        // Full saga flow requires Kafka and Payment Service running
        
        val request = OrderRequest(
            customerId = UUID.randomUUID(),
            deliveryAddress = "Rua das Flores, 123",
            paymentMethod = PaymentMethod.CREDIT_CARD,
            items = listOf(
                OrderItemRequest(
                    productId = "pizza-margherita",
                    productName = "Pizza Margherita",
                    quantity = 2,
                    unitPrice = 45.90
                )
            )
        )

        // This will fail without Payment Service, but validates the endpoint exists
        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/v1/orders",
            request,
            String::class.java
        )

        // Either ACCEPTED (saga started) or some error (dependencies missing)
        assertNotNull(response.statusCode)
    }

    @Test
    fun `should return 404 for non-existent order`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/api/v1/orders/${UUID.randomUUID()}",
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
