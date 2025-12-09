package net.spaceeye.vmod.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.valkyrienskies.core.impl.util.serialization.*
import java.awt.Color

private class ColorSerializer(): JsonSerializer<Color>() {
    override fun handledType(): Class<Color> = Color::class.java
    override fun serialize(value: Color, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("r", value.red)
        gen.writeNumberField("g", value.green)
        gen.writeNumberField("b", value.blue)
        gen.writeEndObject()
    }
}

private class ColorDeserializer(): JsonDeserializer<Color>() {
    override fun handledType(): Class<*> = Color::class.java
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Color? {
        val node = ctxt.readTree(p)
        return Color(
            node.get("r").numberValue().toInt(),
            node.get("g").numberValue().toInt(),
            node.get("b").numberValue().toInt(),
        )
    }
}

class MyVectorSerializer(): JsonSerializer<Vector3d>() {
    override fun handledType(): Class<Vector3d> = Vector3d::class.java
    override fun serialize(value: Vector3d, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("x", value.x)
        gen.writeNumberField("y", value.y)
        gen.writeNumberField("z", value.z)
        gen.writeEndObject()
    }
}

class MyVectorDeserializer(): JsonDeserializer<Vector3d>() {
    override fun handledType(): Class<*> = Vector3d::class.java
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Vector3d? {
        val node = ctxt.readTree(p)
        return Vector3d(
            node.get("x").numberValue(),
            node.get("y").numberValue(),
            node.get("z").numberValue(),
        )
    }
}

fun getMapper(): ObjectMapper {
    return VSJacksonUtil.dtoMapper
        .copy()
        .registerModule(KotlinModule())
        .registerModule(
            SimpleModule()
                .addSerializer(ColorSerializer()).addDeserializer(Color::class.java, ColorDeserializer())
        )
}