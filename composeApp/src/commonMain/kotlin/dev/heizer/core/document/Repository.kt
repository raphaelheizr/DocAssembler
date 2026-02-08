package dev.heizer.core.document

interface Repository<T> {
    fun load(filePath: String): T
    fun save(filePath: String, document: T)
}