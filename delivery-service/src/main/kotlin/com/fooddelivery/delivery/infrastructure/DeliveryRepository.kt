package com.fooddelivery.delivery.infrastructure

import com.fooddelivery.delivery.domain.Delivery
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeliveryRepository : JpaRepository<Delivery, UUID> {
    fun findByOrderId(orderId: UUID): Delivery?
}
