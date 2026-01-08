package com.fooddelivery.shared.avro

import com.fooddelivery.shared.domain.*
import com.fooddelivery.shared.events.KitchenCommand
import com.fooddelivery.shared.events.KitchenEvent
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import java.time.Instant
import java.util.UUID

object KitchenAvroMapper {
    
    fun toAvro(command: KitchenCommand): GenericRecord {
        val record = AvroSchemas.createKitchenCommandRecord()
        
        when (command) {
            is KitchenCommand.PrepareOrder -> {
                record.put("commandType", GenericData.EnumSymbol(
                    record.schema.getField("commandType").schema(), "PREPARE_ORDER"
                ))
                record.put("orderId", command.orderId.toString())
                record.put("timestamp", command.timestamp.toEpochMilli())
                
                val prepareOrderSchema = record.schema.getField("prepareOrder").schema().types[1]
                val prepareOrderRecord = GenericData.Record(prepareOrderSchema)
                
                val itemsSchema = prepareOrderSchema.getField("items").schema()
                val itemSchema = itemsSchema.elementType
                val items = GenericData.Array<GenericRecord>(command.items.size, itemsSchema)
                
                command.items.forEach { item ->
                    val itemRecord = GenericData.Record(itemSchema)
                    itemRecord.put("productId", item.productId.value)
                    itemRecord.put("productName", item.productName)
                    itemRecord.put("quantity", item.quantity)
                    itemRecord.put("unitPrice", item.unitPrice.value.toPlainString())
                    items.add(itemRecord)
                }
                
                prepareOrderRecord.put("items", items)
                prepareOrderRecord.put("priority", command.priority)
                record.put("prepareOrder", prepareOrderRecord)
            }
            is KitchenCommand.CancelTicket -> {
                record.put("commandType", GenericData.EnumSymbol(
                    record.schema.getField("commandType").schema(), "CANCEL_TICKET"
                ))
                record.put("orderId", command.orderId.toString())
                record.put("timestamp", command.timestamp.toEpochMilli())
                
                val cancelTicketSchema = record.schema.getField("cancelTicket").schema().types[1]
                val cancelTicketRecord = GenericData.Record(cancelTicketSchema)
                cancelTicketRecord.put("reason", command.reason)
                record.put("cancelTicket", cancelTicketRecord)
            }
        }
        
        return record
    }
    
    fun fromAvroCommand(record: GenericRecord): KitchenCommand {
        val commandType = record.get("commandType").toString()
        val orderId = OrderId.fromString(record.get("orderId").toString())
        val timestamp = Instant.ofEpochMilli(record.get("timestamp") as Long)
        
        return when (commandType) {
            "PREPARE_ORDER" -> {
                val prepareOrder = record.get("prepareOrder") as GenericRecord
                @Suppress("UNCHECKED_CAST")
                val itemsArray = prepareOrder.get("items") as GenericArray<GenericRecord>
                val items = itemsArray.map { itemRecord ->
                    OrderItem(
                        productId = ProductId(itemRecord.get("productId").toString()),
                        productName = itemRecord.get("productName").toString(),
                        quantity = itemRecord.get("quantity") as Int,
                        unitPrice = MonetaryAmount.of(itemRecord.get("unitPrice").toString())
                    )
                }
                KitchenCommand.PrepareOrder(
                    orderId = orderId,
                    items = items,
                    priority = prepareOrder.get("priority") as Int,
                    timestamp = timestamp
                )
            }
            "CANCEL_TICKET" -> {
                val cancelTicket = record.get("cancelTicket") as GenericRecord
                KitchenCommand.CancelTicket(
                    orderId = orderId,
                    reason = cancelTicket.get("reason").toString(),
                    timestamp = timestamp
                )
            }
            else -> throw IllegalArgumentException("Unknown command type: $commandType")
        }
    }
    
    fun toAvro(event: KitchenEvent): GenericRecord {
        val record = AvroSchemas.createKitchenEventRecord()
        
        record.put("eventId", event.eventId)
        record.put("orderId", event.orderId.toString())
        record.put("timestamp", event.timestamp.toEpochMilli())
        
        when (event) {
            is KitchenEvent.TicketAccepted -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "TICKET_ACCEPTED"
                ))
                val acceptedSchema = record.schema.getField("ticketAccepted").schema().types[1]
                val acceptedRecord = GenericData.Record(acceptedSchema)
                acceptedRecord.put("ticketId", event.ticketId.toString())
                acceptedRecord.put("estimatedPrepTimeMinutes", event.estimatedPrepTimeMinutes)
                record.put("ticketAccepted", acceptedRecord)
            }
            is KitchenEvent.TicketRejected -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "TICKET_REJECTED"
                ))
                val rejectedSchema = record.schema.getField("ticketRejected").schema().types[1]
                val rejectedRecord = GenericData.Record(rejectedSchema)
                rejectedRecord.put("reason", event.reason)
                record.put("ticketRejected", rejectedRecord)
            }
            is KitchenEvent.TicketReady -> {
                record.put("eventType", GenericData.EnumSymbol(
                    record.schema.getField("eventType").schema(), "TICKET_READY"
                ))
                val readySchema = record.schema.getField("ticketReady").schema().types[1]
                val readyRecord = GenericData.Record(readySchema)
                readyRecord.put("ticketId", event.ticketId.toString())
                record.put("ticketReady", readyRecord)
            }
        }
        
        return record
    }
    
    fun fromAvroEvent(record: GenericRecord): KitchenEvent {
        val eventType = record.get("eventType").toString()
        val eventId = record.get("eventId").toString()
        val orderId = OrderId.fromString(record.get("orderId").toString())
        val timestamp = Instant.ofEpochMilli(record.get("timestamp") as Long)
        
        return when (eventType) {
            "TICKET_ACCEPTED" -> {
                val accepted = record.get("ticketAccepted") as GenericRecord
                KitchenEvent.TicketAccepted(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    ticketId = TicketId.fromString(accepted.get("ticketId").toString()),
                    estimatedPrepTimeMinutes = accepted.get("estimatedPrepTimeMinutes") as Int
                )
            }
            "TICKET_REJECTED" -> {
                val rejected = record.get("ticketRejected") as GenericRecord
                KitchenEvent.TicketRejected(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    reason = rejected.get("reason").toString()
                )
            }
            "TICKET_READY" -> {
                val ready = record.get("ticketReady") as GenericRecord
                KitchenEvent.TicketReady(
                    eventId = eventId,
                    timestamp = timestamp,
                    orderId = orderId,
                    ticketId = TicketId.fromString(ready.get("ticketId").toString())
                )
            }
            else -> throw IllegalArgumentException("Unknown event type: $eventType")
        }
    }
}
