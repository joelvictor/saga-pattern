package com.fooddelivery.shared.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import java.io.ByteArrayOutputStream

object AvroSerializer {
    
    fun serialize(record: GenericRecord): ByteArray {
        val writer = GenericDatumWriter<GenericRecord>(record.schema)
        val outputStream = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(outputStream, null)
        writer.write(record, encoder)
        encoder.flush()
        return outputStream.toByteArray()
    }
    
    fun deserialize(bytes: ByteArray, schema: Schema): GenericRecord {
        val reader = GenericDatumReader<GenericRecord>(schema)
        val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
        return reader.read(null, decoder)
    }
}
