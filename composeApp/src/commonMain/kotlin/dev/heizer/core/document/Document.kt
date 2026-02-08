package dev.heizer.core.document

import dev.heizer.core.document.renderer.DocumentRenderer
import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val metadata: Metadata,
    val nodes: List<DocumentNode>
) {
    @Serializable
    data class Metadata(
        val name: String,
        val templatePath: String
    )

    fun save(repository: Repository<Document>) =
        save(repository, OUTPUT_FILE_PATH, this)

    fun render(renderer: DocumentRenderer) {
        renderer.render(this, OUTPUT_FILE_PATH)
    }

    fun addNode(targetId: Long?, newNode: DocumentNode): Document {
        if (targetId == null) {
            return this.copy(nodes = nodes + newNode)
        }
        val newNodes = addChildToNode(nodes, targetId, newNode)
        return this.copy(nodes = newNodes)
    }

    fun removeNode(nodeId: Long): Document {
        val newNodes = removeNodeFromList(nodes, nodeId)
        return this.copy(nodes = newNodes)
    }

    private fun removeNodeFromList(nodes: List<DocumentNode>, nodeId: Long): List<DocumentNode> {
        return nodes.filter { it.id != nodeId }
            .map { node ->
                if (node.children.isNotEmpty()) {
                    node.copy(children = removeNodeFromList(node.children, nodeId))
                } else {
                    node
                }
            }
    }

    private fun addChildToNode(nodes: List<DocumentNode>, targetId: Long, newNode: DocumentNode): List<DocumentNode> {
        return nodes.map { node ->
            if (node.id == targetId) {
                node.copy(children = node.children + newNode)
            } else if (node.children.isNotEmpty()) {
                node.copy(children = addChildToNode(node.children, targetId, newNode))
            } else {
                node
            }
        }
    }

    fun validateTemplates(): List<String> {
        val missingTemplates = mutableListOf<String>()
        fun checkNodes(nodes: List<DocumentNode>) {
            nodes.forEach { node ->
                val file = java.io.File(node.templatePath)
                if (!file.exists()) {
                    missingTemplates.add(node.templatePath)
                }
                checkNodes(node.children)
            }
        }
        checkNodes(this.nodes)
        return missingTemplates
    }

    companion object {
        private const val OUTPUT_FILE_PATH = "../out/output"

        fun load(repository: Repository<Document>, filePath: String): Document =
            repository.load(filePath)

        fun save(repository: Repository<Document>, filePath: String, document: Document) =
            repository.save(filePath, document)

        fun create(name: String, relativePath: String = OUTPUT_FILE_PATH) =
            Document(Metadata(name, relativePath), emptyList())

    }

}
