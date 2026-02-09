package dev.heizer.core.document

import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable

@Serializable
data class DocumentNodeDefinitionRegistry(
    val definitions: List<DocumentNodeDefinition>,
    val customBaseTemplateEnabled: Boolean,
    val customBaseTemplatePath: String,
    val outputPath: String = "",
    val outputFileName: String = "output.docx"
) {
    companion object {
        const val DEFAULT_BASE_TEMPLATE_PATH = "./configs/templates/base-template.docx"
        const val DEFINITIONS_PATH = "./configs/definitions"
    }

    fun save(repository: Repository<DocumentNodeDefinitionRegistry>) =
        repository.save(DEFINITIONS_PATH, this)

    object Factory {
        fun empty(): DocumentNodeDefinitionRegistry =
            DocumentNodeDefinitionRegistry(
                definitions = emptyList(),
                customBaseTemplatePath = DEFAULT_BASE_TEMPLATE_PATH,
                customBaseTemplateEnabled = false
            )

        fun load(repository: Repository<DocumentNodeDefinitionRegistry>): DocumentNodeDefinitionRegistry =
            repository.load(DEFINITIONS_PATH)
    }

}