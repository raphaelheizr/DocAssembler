package dev.heizer.core.document

import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable

@Serializable
class DocumentNodeDefinitionRegistry(
    val definitions: List<DocumentNodeDefinition>
) {
    companion object {
        const val FILE_NAME = "definitions.json"
    }

    fun save(repository: Repository<DocumentNodeDefinitionRegistry>) {
        repository.save(FILE_NAME, this)
    }

    object Factory {
        fun load(repository: Repository<DocumentNodeDefinitionRegistry>): DocumentNodeDefinitionRegistry {
            return repository.load(FILE_NAME)
        }
    }
}