package org.dreamexposure.ticketbird.mapper

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import discord4j.common.util.Snowflake

class SnowflakeMapper: SimpleModule() {
    init {
        addSerializer(SnowflakeSerializer())
        addDeserializer(Snowflake::class.java, SnowflakeDeserializer())
    }

    class SnowflakeSerializer : StdSerializer<Snowflake>(Snowflake::class.java) {
        override fun serialize(value: Snowflake?, gen: JsonGenerator?, provider: SerializerProvider?) {
            gen?.writeString(value?.asString())
        }
    }

    class SnowflakeDeserializer: StdDeserializer<Snowflake>(Snowflake::class.java) {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Snowflake {
            val raw = p?.valueAsString
            return if (raw != null) Snowflake.of(raw) else throw IllegalStateException()
        }
    }
}
