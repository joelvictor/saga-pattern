package com.fooddelivery.kitchen.infrastructure

import com.fooddelivery.kitchen.domain.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TicketRepository : JpaRepository<Ticket, UUID> {
    fun findByOrderId(orderId: UUID): Ticket?
}
