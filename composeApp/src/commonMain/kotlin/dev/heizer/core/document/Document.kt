package dev.heizer.core.document

import dev.heizer.core.document.renderer.DocumentRenderer
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

    companion object {
        private const val OUTPUT_FILE_PATH = "../out/output"

        fun create(name: String, relativePath: String = OUTPUT_FILE_PATH) =
            Document(Metadata(name, relativePath), emptyList())

        @OptIn(ExperimentalUuidApi::class)
        private fun removeNodeFromList(nodes: List<DocumentNode>, nodeId: Uuid): List<DocumentNode> {
            return nodes
                .filter { it.id != nodeId }
                .map { node ->
                    if (node.children.isNotEmpty()) {
                        node.copy(children = removeNodeFromList(node.children, nodeId))
                    } else {
                        node
                    }
                }
        }

        fun checkIfNodeExists(nodes: List<DocumentNode>): Boolean =
            nodes.any { node ->
                java.io.File(node.templatePath).exists() ||
                        checkIfNodeExists(node.children)
            }


        @OptIn(ExperimentalUuidApi::class)
        private fun addChildToNode(nodes: List<DocumentNode>, targetId: Uuid, newNode: DocumentNode): List<DocumentNode> =
            nodes
                .map { node ->
                    when {
                        node.id == targetId ->
                            node.copy(children = node.children + newNode)

                        node.children.isNotEmpty() -> {
                            val updatedChildren = addChildToNode(node.children, targetId, newNode)

                            if (updatedChildren !== node.children)
                                node.copy(children = updatedChildren)
                            else
                                node
                        }

                        else -> node
                    }
                }

    }

    fun render(renderer: DocumentRenderer, outputPath: String, baseTemplatePath: String? = null) {
        renderer.render(this, outputPath, baseTemplatePath)
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

    fun validateTemplates(baseTemplatePath: String? = null): Set<String> {
        val missingTemplates = mutableSetOf<String>()

        baseTemplatePath
            ?.let {
                val file = java.io.File(it)
                if (!file.exists()) {
                    missingTemplates.add(it)
                }
            }

        checkIfNodeExists(this.nodes)
        return missingTemplates
    }

}
