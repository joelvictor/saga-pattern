package com.fooddelivery.shared

import com.fooddelivery.shared.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class DomainPrimitivesTest {

    @Test
    fun `OrderId should wrap UUID correctly`() {
        val uuid = UUID.randomUUID()
        val orderId = OrderId(uuid)
        
        assertEquals(uuid, orderId.value)
        assertEquals(uuid.toString(), orderId.toString())
    }

    @Test
    fun `OrderId generate should create random ID`() {
        val id1 = OrderId.generate()
        val id2 = OrderId.generate()
        
        assert(id1.value != id2.value)
    }

    @Test
    fun `OrderId fromString should parse UUID string`() {
        val uuidString = "550e8400-e29b-41d4-a716-446655440000"
        val orderId = OrderId.fromString(uuidString)
        
        assertEquals(uuidString, orderId.value.toString())
    }

    @Test
    fun `MonetaryAmount should not allow negative values`() {
        assertThrows<IllegalArgumentException> {
            MonetaryAmount(BigDecimal("-10.00"))
        }
    }

    @Test
    fun `MonetaryAmount should allow zero`() {
        val amount = MonetaryAmount(BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, amount.value)
    }

    @Test
    fun `MonetaryAmount plus should add correctly`() {
        val a = MonetaryAmount.of(10.50)
        val b = MonetaryAmount.of(5.25)
        
        val result = a + b
        assertEquals(0, result.value.compareTo(BigDecimal("15.75")))
    }

    @Test
    fun `MonetaryAmount times should multiply by quantity`() {
        val unitPrice = MonetaryAmount.of(25.00)
        val result = unitPrice * 3
        
        assertEquals(0, result.value.compareTo(BigDecimal("75.00")))
    }

    @Test
    fun `MonetaryAmount of should create from double`() {
        val amount = MonetaryAmount.of(99.99)
        assertEquals(BigDecimal.valueOf(99.99), amount.value)
    }

    @Test
    fun `TransactionId should not allow blank values`() {
        assertThrows<IllegalArgumentException> {
            TransactionId("")
        }
        assertThrows<IllegalArgumentException> {
            TransactionId("   ")
        }
    }

    @Test
    fun `ProductId should not allow blank values`() {
        assertThrows<IllegalArgumentException> {
            ProductId("")
        }
    }

    @Test
    fun `Address should not allow blank values`() {
        assertThrows<IllegalArgumentException> {
            Address("")
        }
    }
}
