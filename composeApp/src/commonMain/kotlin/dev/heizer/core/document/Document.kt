package dev.heizer.core.document

import dev.heizer.core.document.renderer.DocumentRenderer
import dev.heizer.core.file.Repository
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    fun render(renderer: DocumentRenderer, baseTemplatePath: String? = null) {
        renderer.render(this, OUTPUT_FILE_PATH, baseTemplatePath)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addNode(targetId: Uuid?, newNode: DocumentNode): Document {
        if (targetId == null) {
            return this.copy(nodes = nodes + newNode)
        }
        val newNodes = addChildToNode(nodes, targetId, newNode)
        return this.copy(nodes = newNodes)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun removeNode(nodeId: Uuid): Document {
        val newNodes = removeNodeFromList(nodes, nodeId)
        return this.copy(nodes = newNodes)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun removeNodeFromList(nodes: List<DocumentNode>, nodeId: Uuid): List<DocumentNode> {
        return nodes.filter { it.id != nodeId }
            .map { node ->
                if (node.children.isNotEmpty()) {
                    node.copy(children = removeNodeFromList(node.children, nodeId))
                } else {
                    node
                }
            }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun addChildToNode(nodes: List<DocumentNode>, targetId: Uuid, newNode: DocumentNode): List<DocumentNode> {
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
        val missingTemplates = mutableSetOf<String>()

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
        return missingTemplates.toList()
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
