package dev.heizer.core.file

interface Repository<T> {
    fun load(filePath: String): T
    fun save(filePath: String, content: T)
}