package com.fooddelivery.order.infrastructure.persistence

import com.fooddelivery.order.domain.Order
import com.fooddelivery.order.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    
    fun findByCustomerId(customerId: UUID): List<Order>
    
    fun findByStatus(status: OrderStatus): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    fun findActiveOrders(): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.status = 'KITCHEN_PENDING' OR o.status = 'DELIVERY_PENDING'")
    fun findPendingOrders(): List<Order>
}
