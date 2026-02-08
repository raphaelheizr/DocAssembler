package dev.heizer.core.file

import dev.heizer.core.serializer.Serializer
import java.io.File

class FileRepository<T>(private val serializer: Serializer<T, String>) : Repository<T> {

    override fun load(filePath: String) =
        File(filePath)
            .let {
                require(it.exists()) { "File does not exist: $filePath" }
                require(it.isFile) { "File is not a file: $filePath" }
                require(it.canRead()) { "File cannot be read: $filePath" }

                val read = it.readText()
                serializer.deserialize(read)
            }

    override fun save(filePath: String, content: T) =
        File(filePath)
            .let {
                require(it.canWrite()) { "File cannot be written: $filePath" }
                it.writeText(serializer.serialize(content))
            }

}