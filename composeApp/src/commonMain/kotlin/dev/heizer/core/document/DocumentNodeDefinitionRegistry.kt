package dev.heizer.core.document

import dev.heizer.core.file.Repository

class DocumentNodeDefinitionRegistry(
    val definitions: List<DocumentNodeDefinition>
) {
    companion object {
        const val FILE_NAME = "definition.json"
    }

    fun save(repository: Repository<DocumentNodeDefinitionRegistry>) {
        repository.save(FILE_NAME, this)
    }

}