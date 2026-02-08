package dev.heizer.core.document

import dev.heizer.core.btree.BTree
import dev.heizer.core.document.renderer.DocumentRenderer
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

    fun render(renderer: DocumentRenderer) {
        renderer.render(this, "../out/output")
    }

    companion object {
        fun load(repository: Repository<Document>, filePath: String): Document =
            repository.load(filePath)

        fun save(repository: Repository<Document>, filePath: String, document: Document) =
            repository.save(filePath, document)

        fun create(name: String, relativePath: String) =
            Document(Metadata(name, relativePath), BTree())

    }

}
