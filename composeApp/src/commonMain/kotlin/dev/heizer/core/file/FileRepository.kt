package dev.heizer.core.file

import dev.heizer.core.serializer.Serializer
import java.io.File

class FileRepository<T>(private val serializer: Serializer<T, String>, private val fileExtension: String? = null) : Repository<T> {

    override fun load(filePath: String) =
        File("$filePath${getFileExtension()}")
            .let {
                require(it.exists()) { "File does not exist: $filePath" }
                require(it.isFile) { "File is not a file: $filePath" }
                require(it.canRead()) { "File cannot be read: $filePath" }

                val read = it.readText()
                serializer.deserialize(read)
            }

    override fun save(filePath: String, content: T) =
        File("$filePath${getFileExtension()}")
            .let {
                it.parentFile?.mkdirs()
                it.writeText(serializer.serialize(content))
            }

    private fun getFileExtension() =
        if (fileExtension != null) ".$fileExtension" else ""
}