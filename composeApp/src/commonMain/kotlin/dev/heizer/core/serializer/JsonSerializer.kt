package dev.heizer.core.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class JsonSerializer<T>(private val kSerializer: KSerializer<T>) : Serializer<T, String> {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    override fun serialize(value: T): String = json.encodeToString(kSerializer, value)

    override fun deserialize(value: String): T = json.decodeFromString(kSerializer, value)

    companion object {
        inline fun <reified T> create(): JsonSerializer<T> = JsonSerializer(serializer<T>())
    }
}
