package dev.heizer.core.document

import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable

@Serializable
data class DocumentNodeDefinitionRegistry(
    val definitions: List<DocumentNodeDefinition>,
    val customBaseTemplateEnabled: Boolean,
    val customBaseTemplatePath: String
) {
    companion object {
        const val DEFINITIONS_PATH = "./configs/definitions.json"
    }

    fun save(repository: Repository<DocumentNodeDefinitionRegistry>) {
        repository.save(DEFINITIONS_PATH, this)
    }

    object Factory {
        fun empty(): DocumentNodeDefinitionRegistry =
            DocumentNodeDefinitionRegistry(
                definitions = emptyList(),
                customBaseTemplatePath = DEFINITIONS_PATH,
                customBaseTemplateEnabled = false
            )

        fun load(repository: Repository<DocumentNodeDefinitionRegistry>): DocumentNodeDefinitionRegistry {
            return repository.load(DEFINITIONS_PATH)
        }
    }

}