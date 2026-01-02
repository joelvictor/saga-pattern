package com.fooddelivery.delivery.domain

import com.fooddelivery.shared.domain.DeliveryStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "deliveries")
class Delivery(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val orderId: UUID,
    
    @Column(nullable = false)
    val deliveryAddress: String,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    
    @Column
    var driverId: String? = null,
    
    @Column
    var driverName: String? = null,
    
    @Column
    var failureReason: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column
    val estimatedPickupTime: Instant? = null,
    
    @Column
    var estimatedDeliveryTime: Instant? = null,
    
    @Column
    var pickedUpAt: Instant? = null,
    
    @Column
    var deliveredAt: Instant? = null
) {
    fun schedule(estimatedDelivery: Instant) {
        this.status = DeliveryStatus.ASSIGNED
        this.driverId = "DRV-${UUID.randomUUID().toString().take(4).uppercase()}"
        this.driverName = "Driver ${(1..100).random()}"
        this.estimatedDeliveryTime = estimatedDelivery
    }
    
    fun pickup() {
        this.status = DeliveryStatus.PICKED_UP
        this.pickedUpAt = Instant.now()
    }
    
    fun startTransit() {
        this.status = DeliveryStatus.IN_TRANSIT
    }
    
    fun complete() {
        this.status = DeliveryStatus.DELIVERED
        this.deliveredAt = Instant.now()
    }
    
    fun fail(reason: String) {
        this.status = DeliveryStatus.FAILED
        this.failureReason = reason
    }
}
