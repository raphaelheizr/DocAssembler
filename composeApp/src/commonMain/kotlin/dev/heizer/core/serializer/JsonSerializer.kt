package dev.heizer.core.serializer

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class JsonSerializer<T>(
    private val json: Json,
    private val serializer: kotlinx.serialization.KSerializer<T>
) : Serializer<T, String> {

    override fun serialize(value: T): String =
        json.encodeToString(serializer, value)

    override fun deserialize(value: String): T = json.decodeFromString(serializer, value)

    companion object {
        inline fun <reified T> create(): JsonSerializer<T> {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                coerceInputValues = true
            }

            return JsonSerializer(json, serializer())
        }
    }
}
