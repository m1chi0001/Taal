package pl.lublin.wsei.plan.data.remote.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/*
 * The APR backend serializes C# `decimal`, enums, `TimeSpan` and nullable value types inconsistently
 * between tenants/versions (e.g. `"ects": "5"` vs `5`, `"priority": 0` vs `"High"`, `"averageGrade": "4,25"`).
 * With plain kotlinx types a single mistyped field makes the whole response fail, which is why whole
 * screens used to stay empty. These serializers accept any JSON primitive and never throw.
 */

/** Any primitive → its text; null → ""; arrays → comma-joined; objects → "". */
object LenientStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("pl.lublin.wsei.plan.LenientString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return lenientText(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/** 5, 5.0, "5", "5,0", "5 h", true → Int; anything unparsable → 0. */
object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("pl.lublin.wsei.plan.LenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return lenientNumber(jsonDecoder.decodeJsonElement())?.toInt() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

object LenientLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("pl.lublin.wsei.plan.LenientLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeLong()
        val element = jsonDecoder.decodeJsonElement()
        // Parse integers directly to keep precision for large ids.
        (element as? JsonPrimitive)?.contentOrNull?.trim()?.toLongOrNull()?.let { return it }
        return lenientNumber(element)?.toLong() ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

/** 4.25, "4.25", "4,25", "1 234,56 PLN" → Double; anything unparsable → 0.0. */
object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("pl.lublin.wsei.plan.LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return lenientNumber(jsonDecoder.decodeJsonElement()) ?: 0.0
    }

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

/** true, "true", "True", 1, "1", "tak", "yes" → true; everything else → false. */
object LenientBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("pl.lublin.wsei.plan.LenientBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return false
        if (primitive is JsonNull) return false
        val text = primitive.content.trim().lowercase()
        return when (text) {
            "true", "tak", "yes", "y", "t" -> true
            "false", "nie", "no", "n", "f", "" -> false
            else -> (text.replace(',', '.').toDoubleOrNull() ?: 0.0) != 0.0
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}

typealias LString = @Serializable(with = LenientStringSerializer::class) String
typealias LInt = @Serializable(with = LenientIntSerializer::class) Int
typealias LLong = @Serializable(with = LenientLongSerializer::class) Long
typealias LDouble = @Serializable(with = LenientDoubleSerializer::class) Double
typealias LBool = @Serializable(with = LenientBooleanSerializer::class) Boolean

private val NUMBER_PATTERN = Regex("""[-+]?\d+(?:[.,]\d+)?""")

internal fun lenientText(element: JsonElement): String = when (element) {
    is JsonNull -> ""
    is JsonPrimitive -> element.content
    is JsonArray -> element.joinToString(", ") { lenientText(it) }.trim()
    is JsonObject -> ""
}

internal fun lenientNumber(element: JsonElement): Double? {
    val primitive = element as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val raw = primitive.content.trim()
    when (raw.lowercase()) {
        "true" -> return 1.0
        "false" -> return 0.0
    }
    raw.toDoubleOrNull()?.let { return it }
    // "1 234,56 PLN" → "1234,56" → 1234.56
    val compact = raw.replace(" ", "").replace(" ", "")
    return NUMBER_PATTERN.find(compact)?.value?.replace(',', '.')?.toDoubleOrNull()
}
