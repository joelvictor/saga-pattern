package com.fooddelivery.shared.avro

import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.OrderEvent
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import java.time.Instant

object OrderAvroMapper {
    
    fun toAvro(event: OrderEvent): GenericRecord {
        val record = AvroSchemas.createOrderEventRecord()
        
        record.put("eventId", event.eventId)
        record.put("orderId", event.orderId.toString())
        record.put("timestamp", event.timestamp.toEpochMilli())
        
        when (event) {
            is OrderEvent.OrderCreated -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "ORDER_CREATED"
                ))
                val createdSchema = record.schema.getField("orderCreated").schema().types[1]
                val createdRecord = GenericData.Record(createdSchema)
                createdRecord.put("customerId", event.customerId.toString())
                createdRecord.put("totalAmount", event.totalAmount.value.toPlainString())
                record.put("orderCreated", createdRecord)
            }
            is OrderEvent.OrderCompleted -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "ORDER_COMPLETED"
                ))
                val completedSchema = record.schema.getField("orderCompleted").schema().types[1]
                val completedRecord = GenericData.Record(completedSchema)
                completedRecord.put("completedAt", event.completedAt.toEpochMilli())
                record.put("orderCompleted", completedRecord)
            }
            is OrderEvent.OrderCancelled -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "ORDER_CANCELLED"
                ))
                val cancelledSchema = record.schema.getField("orderCancelled").schema().types[1]
                val cancelledRecord = GenericData.Record(cancelledSchema)
                cancelledRecord.put("reason", event.reason)
                record.put("orderCancelled", cancelledRecord)
            }
        }
        
        return record
    }
    
    fun fromAvroEvent(record: GenericRecord): OrderEvent {
        val eventType = record.get("eventType").toString()
        val eventId = record.get("eventId").toString()
        val orderId = OrderId.fromString(record.get("orderId").toString())
        val timestamp = Instant.ofEpochMilli(record.get("timestamp") as Long)
        
        return when (eventType) {
            "ORDER_CREATED" -> {
                val created = record.get("orderCreated") as GenericRecord
                OrderEvent.OrderCreated(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    customerId = CustomerId.fromString(created.get("customerId").toString()),
                    totalAmount = MonetaryAmount.of(created.get("totalAmount").toString())
                )
            }
            "ORDER_COMPLETED" -> {
                val completed = record.get("orderCompleted") as GenericRecord
                OrderEvent.OrderCompleted(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    completedAt = Instant.ofEpochMilli(completed.get("completedAt") as Long)
                )
            }
            "ORDER_CANCELLED" -> {
                val cancelled = record.get("orderCancelled") as GenericRecord
                OrderEvent.OrderCancelled(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    reason = cancelled.get("reason").toString()
                )
            }
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
}
