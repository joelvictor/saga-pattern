package com.fooddelivery.kitchen.domain

import com.fooddelivery.shared.domain.OrderItem
import com.fooddelivery.shared.domain.TicketStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tickets")
class Ticket(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val orderId: UUID,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TicketStatus = TicketStatus.PENDING,
    
    @Column
    var estimatedPrepTimeMinutes: Int = 0,
    
    @Column
    var rejectionReason: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column
    var acceptedAt: Instant? = null,
    
    @Column
    var readyAt: Instant? = null,
    
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_id")
    val items: MutableList<TicketItem> = mutableListOf()
) {
    fun accept(estimatedMinutes: Int) {
        this.status = TicketStatus.ACCEPTED
        this.estimatedPrepTimeMinutes = estimatedMinutes
        this.acceptedAt = Instant.now()
    }
    
    fun reject(reason: String) {
        this.status = TicketStatus.REJECTED
        this.rejectionReason = reason
    }
    
    fun startPreparing() {
        this.status = TicketStatus.PREPARING
    }
    
    fun markReady() {
        this.status = TicketStatus.READY
        this.readyAt = Instant.now()
    }
}

@Entity
@Table(name = "ticket_items")
class TicketItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val productId: String,
    
    @Column(nullable = false)
    val productName: String,
    
    @Column(nullable = false)
    val quantity: Int
)
