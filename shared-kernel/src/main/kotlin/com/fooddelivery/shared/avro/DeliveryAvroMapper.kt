package com.fooddelivery.shared.avro

import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.DeliveryCommand
import com.fooddelivery.shared.events.DeliveryEvent
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import java.time.Instant

object DeliveryAvroMapper {
    
    fun toAvro(command: DeliveryCommand): GenericRecord {
        val record = AvroSchemas.createDeliveryCommandRecord()
        
        when (command) {
            is DeliveryCommand.ScheduleDelivery -> {
                record.put("commandType", GenericData.EnumSymbol(
                    record.schema.getField("commandType").schema(), "SCHEDULE_DELIVERY"
                ))
                record.put("orderId", command.orderId.toString())
                record.put("timestamp", command.timestamp.toEpochMilli())
                
                val scheduleSchema = record.schema.getField("scheduleDelivery").schema().types[1]
                val scheduleRecord = GenericData.Record(scheduleSchema)
                scheduleRecord.put("deliveryAddress", command.deliveryAddress.value)
                scheduleRecord.put("estimatedPickupTime", command.estimatedPickupTime.toEpochMilli())
                record.put("scheduleDelivery", scheduleRecord)
            }
            is DeliveryCommand.CancelDelivery -> {
                record.put("commandType", GenericData.EnumSymbol(
                    record.schema.getField("commandType").schema(), "CANCEL_DELIVERY"
                ))
                record.put("orderId", command.orderId.toString())
                record.put("timestamp", command.timestamp.toEpochMilli())
                
                val cancelSchema = record.schema.getField("cancelDelivery").schema().types[1]
                val cancelRecord = GenericData.Record(cancelSchema)
                cancelRecord.put("reason", command.reason)
                record.put("cancelDelivery", cancelRecord)
            }
        }
        
        return record
    }
    
    fun fromAvroCommand(record: GenericRecord): DeliveryCommand {
        val commandType = record.get("commandType").toString()
        val orderId = OrderId.fromString(record.get("orderId").toString())
        val timestamp = Instant.ofEpochMilli(record.get("timestamp") as Long)
        
        return when (commandType) {
            "SCHEDULE_DELIVERY" -> {
                val schedule = record.get("scheduleDelivery") as GenericRecord
                DeliveryCommand.ScheduleDelivery(
                    orderId = orderId,
                    deliveryAddress = Address(schedule.get("deliveryAddress").toString()),
                    estimatedPickupTime = Instant.ofEpochMilli(schedule.get("estimatedPickupTime") as Long),
                    timestamp = timestamp
                )
            }
            "CANCEL_DELIVERY" -> {
                val cancel = record.get("cancelDelivery") as GenericRecord
                DeliveryCommand.CancelDelivery(
                    orderId = orderId,
                    reason = cancel.get("reason").toString(),
                    timestamp = timestamp
                )
            }
            else -> throw IllegalArgumentException("Unknown command type: $commandType")
        }
    }
    
    fun toAvro(event: DeliveryEvent): GenericRecord {
        val record = AvroSchemas.createDeliveryEventRecord()
        
        record.put("eventId", event.eventId)
        record.put("orderId", event.orderId.toString())
        record.put("timestamp", event.timestamp.toEpochMilli())
        
        when (event) {
            is DeliveryEvent.DeliveryScheduled -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "DELIVERY_SCHEDULED"
                ))
                val scheduledSchema = record.schema.getField("deliveryScheduled").schema().types[1]
                val scheduledRecord = GenericData.Record(scheduledSchema)
                scheduledRecord.put("deliveryId", event.deliveryId.toString())
                scheduledRecord.put("estimatedDeliveryTime", event.estimatedDeliveryTime.toEpochMilli())
                record.put("deliveryScheduled", scheduledRecord)
            }
            is DeliveryEvent.DeliveryPickedUp -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "DELIVERY_PICKED_UP"
                ))
                val pickedUpSchema = record.schema.getField("deliveryPickedUp").schema().types[1]
                val pickedUpRecord = GenericData.Record(pickedUpSchema)
                pickedUpRecord.put("deliveryId", event.deliveryId.toString())
                record.put("deliveryPickedUp", pickedUpRecord)
            }
            is DeliveryEvent.DeliveryCompleted -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "DELIVERY_COMPLETED"
                ))
                val completedSchema = record.schema.getField("deliveryCompleted").schema().types[1]
                val completedRecord = GenericData.Record(completedSchema)
                completedRecord.put("deliveryId", event.deliveryId.toString())
                completedRecord.put("completedAt", event.completedAt.toEpochMilli())
                record.put("deliveryCompleted", completedRecord)
            }
            is DeliveryEvent.DeliveryFailed -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "DELIVERY_FAILED"
                ))
                val failedSchema = record.schema.getField("deliveryFailed").schema().types[1]
                val failedRecord = GenericData.Record(failedSchema)
                failedRecord.put("deliveryId", event.deliveryId.toString())
                failedRecord.put("reason", event.reason)
                record.put("deliveryFailed", failedRecord)
            }
        }
        
        return record
    }
    
    fun fromAvroEvent(record: GenericRecord): DeliveryEvent {
        val eventType = record.get("eventType").toString()
        val eventId = record.get("eventId").toString()
        val orderId = OrderId.fromString(record.get("orderId").toString())
        val timestamp = Instant.ofEpochMilli(record.get("timestamp") as Long)
        
        return when (eventType) {
            "DELIVERY_SCHEDULED" -> {
                val scheduled = record.get("deliveryScheduled") as GenericRecord
                DeliveryEvent.DeliveryScheduled(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    deliveryId = DeliveryId.fromString(scheduled.get("deliveryId").toString()),
                    estimatedDeliveryTime = Instant.ofEpochMilli(scheduled.get("estimatedDeliveryTime") as Long)
                )
            }
            "DELIVERY_PICKED_UP" -> {
                val pickedUp = record.get("deliveryPickedUp") as GenericRecord
                DeliveryEvent.DeliveryPickedUp(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    deliveryId = DeliveryId.fromString(pickedUp.get("deliveryId").toString())
                )
            }
            "DELIVERY_COMPLETED" -> {
                val completed = record.get("deliveryCompleted") as GenericRecord
                DeliveryEvent.DeliveryCompleted(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    deliveryId = DeliveryId.fromString(completed.get("deliveryId").toString()),
                    completedAt = Instant.ofEpochMilli(completed.get("completedAt") as Long)
                )
            }
            "DELIVERY_FAILED" -> {
                val failed = record.get("deliveryFailed") as GenericRecord
                DeliveryEvent.DeliveryFailed(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    deliveryId = DeliveryId.fromString(failed.get("deliveryId").toString()),
                    reason = failed.get("reason").toString()
                )
            }
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
}
