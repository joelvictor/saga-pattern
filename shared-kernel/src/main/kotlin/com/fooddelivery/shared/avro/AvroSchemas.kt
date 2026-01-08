package com.fooddelivery.shared.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord

object AvroSchemas {
    
    val KITCHEN_COMMAND_SCHEMA: Schema by lazy {
        Schema.Parser().parse(
            AvroSchemas::class.java.getResourceAsStream("/avro/kitchen-commands.avsc")
        )
    }
    
    val KITCHEN_EVENT_SCHEMA: Schema by lazy {
        Schema.Parser().parse(
            AvroSchemas::class.java.getResourceAsStream("/avro/kitchen-events.avsc")
        )
    }
    
    val DELIVERY_COMMAND_SCHEMA: Schema by lazy {
        Schema.Parser().parse(
            AvroSchemas::class.java.getResourceAsStream("/avro/delivery-commands.avsc")
        )
    }
    
    val DELIVERY_EVENT_SCHEMA: Schema by lazy {
        Schema.Parser().parse(
            AvroSchemas::class.java.getResourceAsStream("/avro/delivery-events.avsc")
        )
    }
    
    val ORDER_EVENT_SCHEMA: Schema by lazy {
        Schema.Parser().parse(
            AvroSchemas::class.java.getResourceAsStream("/avro/order-events.avsc")
        )
    }
    
    fun createKitchenCommandRecord(): GenericRecord = GenericData.Record(KITCHEN_COMMAND_SCHEMA)
    fun createKitchenEventRecord(): GenericRecord = GenericData.Record(KITCHEN_EVENT_SCHEMA)
    fun createDeliveryCommandRecord(): GenericRecord = GenericData.Record(DELIVERY_COMMAND_SCHEMA)
    fun createDeliveryEventRecord(): GenericRecord = GenericData.Record(DELIVERY_EVENT_SCHEMA)
    fun createOrderEventRecord(): GenericRecord = GenericData.Record(ORDER_EVENT_SCHEMA)
}
