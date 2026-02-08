package dev.heizer.core.document

import dev.heizer.core.btree.BTree
import dev.heizer.core.document.renderer.DocumentRenderer
import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val metadata: Metadata,
    val nodes: BTree<DocumentNode>
) {
    @Serializable
    data class Metadata(
        val name: String,
        val relativePath: String
    )

    fun save(repository: Repository<Document>) =
        save(repository, OUTPUT_FILE_PATH, this)

    fun render(renderer: DocumentRenderer) {
        renderer.render(this, OUTPUT_FILE_PATH)
    }

    companion object {
        private const val OUTPUT_FILE_PATH = "../out/output"

        fun load(repository: Repository<Document>, filePath: String): Document =
            repository.load(filePath)

        fun save(repository: Repository<Document>, filePath: String, document: Document) =
            repository.save(filePath, document)

        fun create(name: String, relativePath: String = OUTPUT_FILE_PATH) =
            Document(Metadata(name, relativePath), BTree())

    }

}
