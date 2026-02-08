package dev.heizer.core.serializer

interface Serializer<T, R> {
    fun serialize(value: T): R
    fun deserialize(value: R): T
}